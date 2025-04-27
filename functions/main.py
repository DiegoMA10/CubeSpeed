import math
import statistics
from typing import Dict, List, Any, Optional

from firebase_functions.firestore_fn import (
  on_document_written,
  on_document_deleted,
  Event,
  Change,
  DocumentSnapshot,
)

from firebase_functions import firestore_fn, https_fn
from firebase_admin import initialize_app, firestore
from google.cloud import exceptions as gexc
from google.cloud.firestore_v1 import aggregation
from google.cloud.firestore_v1.base_query import FieldFilter

# Initialize Firebase Admin SDK
initialize_app()


def get_stats_aggregated(user_id: str, cube_type: str, tag_id: str):
    db = firestore.Client()
    # Get a reference to the collection
    collection_ref = db.collection("users").document(user_id).collection("timers")
    query = collection_ref.where(filter=FieldFilter("cube", "==", cube_type)).where(filter=FieldFilter("tagId", "==", tag_id))

    # 1) COUNT - Using AggregationQuery with alias
    count_aggregate_query = aggregation.AggregationQuery(query)
    count_aggregate_query.count(alias="all")

    count_results = count_aggregate_query.get()
    total_solves = 0
    for result in count_results:
        if result[0].alias == "all":
            total_solves = result[0].value

    # 2) AVG - Using AggregationQuery with alias
    try:
        avg_aggregate_query = aggregation.AggregationQuery(query)
        avg_aggregate_query.avg("duration", alias="average")

        avg_results = avg_aggregate_query.get()
        avg_duration = 0.0
        for result in avg_results:
            if result[0].alias == "average":
                avg_duration = result[0].value if result[0].value is not None else 0.0
    except Exception as e:
        print(f"Error calculating average using aggregate: {e}")
        avg_duration = 0.0

    # 3) BEST - Get the best document (lowest duration)
    try:
        best_docs = query.order_by(
            field_path="duration",
            direction=firestore.Query.ASCENDING
        ).limit(1).get()

        best = None
        for doc in best_docs:
            best = doc.to_dict()  # Get the best solve (lowest duration)
    except Exception as e:
        print(f"Error getting best solve: {e}")
        best = None

    return {"count": total_solves, "average": avg_duration, "best": best}



# Function to recalculate statistics for a user, cube type, and tag
def recalculate_stats(user_id: str, cube_type: str, tag_id: str) -> None:
    """
    Recalculates statistics for a user, cube type, and tag.
    This function is called when a solve is added, updated, or deleted.
    """
    print(f"Recalculating stats for user {user_id}, cube {cube_type}, tag {tag_id}")

    db = firestore.Client()
    stats_ref = db.collection("users").document(user_id).collection("stats")
    stats_doc_id = f"{cube_type}_{tag_id}"

    # Definimos la referencia a la colección de timers
    base_q = (
        db.collection("users")
        .document(user_id)
        .collection("timers")
        .where(filter=firestore.FieldFilter("cube", "==", cube_type))
        .where(filter=firestore.FieldFilter("tagId", "==", tag_id))
    )

    # Usamos get_stats_aggregated para obtener estadísticas básicas
    aggregated_stats = get_stats_aggregated(user_id, cube_type, tag_id)

    # Obtenemos las estadísticas actuales para campos adicionales
    stats_doc = stats_ref.document(stats_doc_id).get()
    current_stats = stats_doc.to_dict() if stats_doc.exists else {}

    # Usamos los valores de las estadísticas agregadas y nos aseguramos que sean números
    count = int(aggregated_stats["count"])
    average = float(aggregated_stats["average"])
    best = aggregated_stats["best"]
    deviation = 0

    # Calculamos valid_count contando solves que no son DNF
    try:
        valid_count_query = (
            db.collection("users").document(user_id).collection("timers")
            .where(filter=firestore.FieldFilter("cube", "==", cube_type))
            .where(filter=firestore.FieldFilter("tagId", "==", tag_id))
            .where(filter=firestore.FieldFilter("status", "!=", "DNF"))
        )

        # Usando AggregationQuery para contar
        valid_agg_query = aggregation.AggregationQuery(valid_count_query)
        valid_agg_query.count(alias="validCount")

        valid_count_results = valid_agg_query.get()
        num_valid_solves = 0
        for result in valid_count_results:
            if result[0].alias == "validCount":
                num_valid_solves = int(result[0].value)
    except Exception as e:
        # Si falla la agregación, usamos el valor actual o estimamos
        print(f"[recalculate_stats] Error al calcular valid_count: {e}")
        num_valid_solves = int(current_stats.get("validCount", 0))
        if not num_valid_solves and count > 0:
            num_valid_solves = count  # Estimación

    # Calculamos sum_time para estadísticas incrementales
    sum_time = 0
    if average > 0 and num_valid_solves > 0:
        sum_time = average * num_valid_solves

    # Para AoN (móviles) sólo leo las últimas 100 solves
    try:
        solves_query = (
            base_q
            .order_by(field_path="timestamp", direction=firestore.Query.DESCENDING)
            .limit(100)
        )

        solves_docs = solves_query.get()
        solves = []
        for doc in solves_docs:
            solve = doc.to_dict()
            if solve:
                solves.append(solve)
    except gexc.FailedPrecondition as e:
        # Índice aún BUILDING, salimos sin interrumpir todo
        print(f"[recalculate_stats] Índice aún en construcción: {e}")
        return

    valid_solves = [s for s in solves if s.get("status") != "DNF"]
    times = [float(s.get("duration", 0)) for s in valid_solves]

    # Calculamos la desviación estándar
    deviation = 0
    if len(times) >= 2:  # La desviación estándar requiere al menos 2 valores
        try:
            deviation = statistics.stdev(times)
        except Exception as e:
            print(f"[recalculate_stats] Error al calcular la desviación estándar: {e}")
            deviation = 0

    # Creamos el objeto de estadísticas
    stats = {
        "count": count,
        "validCount": num_valid_solves,
        "sum": sum_time,
        "best": best.get("duration") if best else 0,
        "average": average,
        "deviation": deviation,
        "ao5": calculate_average_of_n(times, 5),
        "ao12": calculate_average_of_n(times, 12),
        "ao50": calculate_average_of_n(times, 50),
        "ao100": calculate_average_of_n(times, 100)
    }

    # Guardamos las estadísticas
    stats_ref.document(stats_doc_id).set(stats)


# Firestore trigger function that runs when a solve is added or updated
@on_document_written(document="users/{userId}/timers/{timerId}")
def update_stats_on_solve(event: Event[DocumentSnapshot | None]) -> None:
    user_id = event.params["userId"]

    # Handle document deletion - this is now handled by update_stats_on_delete
    if not event.data.after.exists:
        return

    # Handle document creation or update
    solve_data = event.data.after.to_dict()
    if not solve_data:
        return

    cube_type = solve_data.get("cube")
    tag_id = solve_data.get("tagId")
    if not cube_type or not tag_id:
        return

    # For creation or update, recalculate stats
    recalculate_stats(user_id, cube_type, tag_id)


# Firestore trigger function that runs when a solve is deleted
@on_document_deleted(document="users/{userId}/timers/{timerId}")
def update_stats_on_delete(event: Event[DocumentSnapshot]) -> None:
    user_id = event.params["userId"]

    # Get data from the snapshot (which contains the document before deletion)
    solve_data = event.data.to_dict()
    if not solve_data:
        return

    cube_type = solve_data.get("cube")
    tag_id = solve_data.get("tagId")
    if not cube_type or not tag_id:
        return

    print(f"Document deleted. Recalculating stats for user {user_id}, cube {cube_type}, tag {tag_id}")
    # Recalculate stats after deletion
    recalculate_stats(user_id, cube_type, tag_id)


def calculate_average_of_n(times: List[float], n: int) -> float:
    """
    Calculates the average of N times, excluding the best and worst times.
    Returns 0 if there are not enough times.
    """
    if len(times) < n:
        return 0

    # Get the most recent n times
    recent_times = times[:n]

    if n <= 2:
        # For n <= 2, just return the average
        return sum(recent_times) / len(recent_times)

    # For n > 2, exclude best and worst
    sorted_times = sorted(recent_times)
    trimmed_times = sorted_times[1:-1]  # Remove best and worst
    return sum(trimmed_times) / len(trimmed_times)


# HTTP function to get statistics for a user
@https_fn.on_request()
def get_stats(req: https_fn.Request) -> https_fn.Response:
    """
    HTTP function to get statistics for a user.
    Query parameters:
    - userId: the user ID
    - cubeType: the cube type
    - tagId: the tag ID
    Returns a JSON object with the statistics.
    """
    # Get query parameters
    user_id = req.args.get("userId")
    cube_type = req.args.get("cubeType")
    tag_id = req.args.get("tagId")

    if not user_id or not cube_type or not tag_id:
        return https_fn.Response(
            json={"error": "Missing required parameters"},
            status=400
        )

    # Get statistics from Firestore
    db = firestore.Client()
    stats_ref = db.collection("users").document(user_id).collection("stats")
    stats_doc_id = f"{cube_type}_{tag_id}"
    stats_doc = stats_ref.document(stats_doc_id).get()

    if not stats_doc.exists:
        # Return empty statistics if not found
        return https_fn.Response(
            json={
                "count": 0,
                "validCount": 0,
                "best": 0,
                "average": 0,
                "deviation": 0,
                "ao5": 0,
                "ao12": 0,
                "ao50": 0,
                "ao100": 0
            }
        )

    # Return statistics
    return https_fn.Response(json=stats_doc.to_dict())
