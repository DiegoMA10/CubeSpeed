using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class RubikCrossPlatform : MonoBehaviour
{
    public float minimumDragPixels = 10.0f; // Aumentado para exigir movimientos más claros
    public float normalSnapSpeed = 400.0f; // Velocidad de giro normal
    public float shuffleSnapSpeed = 1000.0f; // Velocidad de giro durante el shuffle
    public float snapSpeed { get { return isShuffling ? shuffleSnapSpeed : normalSnapSpeed; } } // Velocidad de giro variable
    public float touchDeadzone = 0.3f; // Zona muerta para evitar movimientos accidentales
    public bool stricter2DSwipes = true; // Para movimientos más estrictos en 2D
    public static bool isRotatingFace = false;
    public static bool isShuffling = false; // Indica si el cubo está siendo barajado

    // Reference to the CubeStateDetector component
    private CubeStateDetector cubeStateDetector;

    // Event to notify when a manual face rotation is completed
    // The parameters are the face that was rotated and whether it was clockwise
    public event System.Action<char, bool> OnManualRotationCompleted;

    // Property to check if the cube is currently moving (used by ImprovedCubeSolver)
    public bool isMoving { get { return isRotatingFace; } }

    Camera _camera;
    Collider[] _subCubes = new Collider[54];
    Vector3[] _originalPositions = new Vector3[54];
    Quaternion[] _originalOrientations = new Quaternion[54];

    Vector2 initialTouchPosition;

    IEnumerator Start()
    {
        // Set target frame rate to 60 FPS
        Application.targetFrameRate = 60;

        _camera = Camera.main;
        Vector3 camForward = _camera.transform.forward;
        float axisSign = Mathf.Sign(camForward.x * camForward.y * camForward.z);

        // Asegurar que todas las piezas tengan el componente Cubito
        AddCubitoComponentToAllPieces();

        // Initialize CubeStateDetector reference
        InitializeCubeStateDetector();

        while (true)
        {
            yield return null;

            // Si ya estamos rotando el cubo entero, está en animación, o la rotación comenzó fuera del cubo,
            // no procesamos rotación de caras
            if (RotateBigCube.isRotatingCube || RotateBigCube.isAutoRotating || RotateBigCube.rotationStartedOutsideCube)
            {
                continue;
            }

            // Detectar inicio de entrada (toque o click)
            if ((Input.touchCount > 0 && Input.GetTouch(0).phase == TouchPhase.Began) ||
                Input.GetMouseButtonDown(0))
            {
                Vector2 inputPosition;
                if (Input.touchCount > 0)
                    inputPosition = Input.GetTouch(0).position;
                else
                    inputPosition = Input.mousePosition;

                initialTouchPosition = inputPosition;

                // Lanzar un rayo para ver si tocamos el cubo
                Ray ray = _camera.ScreenPointToRay(inputPosition);
                if (Physics.Raycast(ray, out RaycastHit hit))
                {
                    // Si tocamos un cubo, preparamos para posible rotación de cara

                    // El resto del procesamiento se hará cuando el usuario arrastre o suelte
                    yield return StartCoroutine(HandleFaceRotation(hit, inputPosition));
                }
                // Si el rayo no golpea nada, dejamos que RotateBigCube maneje esto
            }
        }
    }

    // Método para agregar componente Cubito a todas las piezas
    private void AddCubitoComponentToAllPieces()
    {
        foreach (Transform child in transform)
        {
            if (!child.GetComponent<Cubito>())
            {
                child.gameObject.AddComponent<Cubito>();
                // El Awake se llama automáticamente y guarda la posición inicial
            }
        }
    }

    // Método para inicializar la referencia a CubeStateDetector
    private void InitializeCubeStateDetector()
    {
        // Try to get the CubeStateDetector component from this GameObject
        cubeStateDetector = GetComponent<CubeStateDetector>();

        // If that fails, try to get it from a child GameObject
        if (cubeStateDetector == null)
        {
            cubeStateDetector = GetComponentInChildren<CubeStateDetector>();

            // If that fails, try to find it in the scene
            if (cubeStateDetector == null)
            {
                cubeStateDetector = FindFirstObjectByType<CubeStateDetector>();

                // If that fails, create a new one
                if (cubeStateDetector == null)
                {
                    // Debug.LogWarning("No CubeStateDetector found in the scene. Creating one...");

                    // Create a new GameObject for the CubeStateDetector
                    GameObject detectorObj = new GameObject("CubeStateDetector");
                    detectorObj.transform.SetParent(transform);
                    cubeStateDetector = detectorObj.AddComponent<CubeStateDetector>();
                }
            }
        }
    }

    /// <summary>
    /// Detects the current state of the cube using the CubeStateDetector component
    /// </summary>
    /// <returns>A 54-character string representing the cube state, or a solved state if detection fails</returns>
    private string DetectCubeState()
    {
        // Solved state representation
        const string SOLVED_STATE = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";

        try
        {
            // Use the CubeStateDetector component
            if (cubeStateDetector != null)
            {
                string detectedState = cubeStateDetector.DetectCubeState();
                if (!string.IsNullOrEmpty(detectedState))
                {
                    // Debug.Log("Cube state detected by CubeStateDetector: " + detectedState);
                    return detectedState;
                }
                else
                {
                    // Debug.LogWarning("CubeStateDetector returned null or empty state. Using solved state as fallback.");
                    return SOLVED_STATE;
                }
            }
            else
            {
                // Debug.LogWarning("CubeStateDetector is null. Using solved state as fallback.");
                return SOLVED_STATE;
            }
        }
        catch (System.Exception ex)
        {
            // Debug.LogError("Error detecting cube state: " + ex.Message);
            return SOLVED_STATE;
        }
    }

    IEnumerator HandleFaceRotation(RaycastHit hit, Vector2 inputPosition)
    {
        // Prevent manual rotations when the cube is being solved or shuffled
        if (isShuffling)
        {
            yield break;
        }

        Vector3 camForward = _camera.transform.forward;
        float axisSign = Mathf.Sign(camForward.x * camForward.y * camForward.z);

        // Determinar ejes de rotación basados en la cara golpeada
        // Determinar qué eje es el dominante en la normal
        Vector3 absNormal = new Vector3(Mathf.Abs(hit.normal.x), Mathf.Abs(hit.normal.y), Mathf.Abs(hit.normal.z));
        int normalAxis;

        // Encontrar el eje dominante (el que tiene el mayor valor absoluto)
        if (absNormal.x >= absNormal.y && absNormal.x >= absNormal.z)
            normalAxis = 0; // X es dominante
        else if (absNormal.y >= absNormal.x && absNormal.y >= absNormal.z)
            normalAxis = 1; // Y es dominante
        else
            normalAxis = 2; // Z es dominante

        Vector3 rotationAxis = Vector3.zero;
        Vector3 alternativeAxis = Vector3.zero;
        rotationAxis[(normalAxis + 1) % 3] = 1;
        alternativeAxis[(normalAxis + 2) % 3] = 1;

        // Ajustar dirección de rotación basada en la cámara
        float signFlip = axisSign * Mathf.Sign(Vector3.Dot(rotationAxis, camForward) *
                                               Mathf.Sign(Vector3.Dot(alternativeAxis, camForward)));
        Vector2 rotationDirection = signFlip * ScreenDirection(inputPosition, hit.point, alternativeAxis);
        Vector2 alternativeDirection = -signFlip * ScreenDirection(inputPosition, hit.point, rotationAxis);

        // Esperar para ver si el usuario arrastra lo suficiente para rotar una cara
        Vector3 selectedAxis = Vector3.zero;
        Vector2 selectedDirection = Vector2.zero;
        float signedDistance = 0f;
        bool startedRotation = false;

        // Para almacenar la posición actual en cada frame
        Vector2 currentPosition = inputPosition;

        // Historial de las últimas posiciones para promediar el movimiento (mejora estabilidad)
        List<Vector2> positionHistory = new List<Vector2>();

        // Temporizador para cancelar y dejar que RotateBigCube lo maneje si no hay arrastre significativo
        float waitStartTime = Time.time;

        while ((Input.touchCount > 0 && Input.GetTouch(0).phase != TouchPhase.Ended) ||
               Input.GetMouseButton(0))
        {
            // Si el cubo comienza a rotar automáticamente o la rotación comenzó fuera del cubo, cancelamos
            if (RotateBigCube.isAutoRotating || RotateBigCube.rotationStartedOutsideCube)
            {
                yield break;
            }

            // Obtener posición actual
            if (Input.touchCount > 0)
                currentPosition = Input.GetTouch(0).position;
            else
                currentPosition = Input.mousePosition;

            // Añadir al historial (hasta 5 posiciones para promediar el movimiento)
            positionHistory.Add(currentPosition);
            if (positionHistory.Count > 5)
                positionHistory.RemoveAt(0);

            // Calcular posición promedio para suavizar temblores
            Vector2 avgPosition = Vector2.zero;
            foreach (Vector2 pos in positionHistory)
                avgPosition += pos;
            avgPosition /= positionHistory.Count;

            // Vector de desplazamiento total
            Vector2 totalDelta = avgPosition - inputPosition;
            float totalDistance = totalDelta.magnitude;

            // Si el movimiento total es suficiente, determinar la dirección principal
            if (totalDistance > minimumDragPixels)
            {
                // Para movimientos más estrictos en 2D (mejor para móviles)
                if (stricter2DSwipes)
                {
                    // Determinar si el movimiento es más horizontal o vertical
                    bool isHorizontalMove = Mathf.Abs(totalDelta.x) > Mathf.Abs(totalDelta.y) * (1 + touchDeadzone);
                    bool isVerticalMove = Mathf.Abs(totalDelta.y) > Mathf.Abs(totalDelta.x) * (1 + touchDeadzone);

                    if (isHorizontalMove)
                    {
                        // Movimiento horizontal - usar eje que mejor se alinee
                        float dotMain = Mathf.Abs(Vector2.Dot(totalDelta.normalized, rotationDirection));
                        float dotAlt = Mathf.Abs(Vector2.Dot(totalDelta.normalized, alternativeDirection));

                        if (dotMain > dotAlt)
                        {
                            selectedAxis = rotationAxis;
                            selectedDirection = rotationDirection;
                            signedDistance = DistanceAlong(inputPosition, avgPosition, rotationDirection);
                        }
                        else
                        {
                            selectedAxis = alternativeAxis;
                            selectedDirection = alternativeDirection;
                            signedDistance = DistanceAlong(inputPosition, avgPosition, alternativeDirection);
                        }

                        startedRotation = true;
                        break;
                    }
                    else if (isVerticalMove)
                    {
                        // Movimiento vertical - usar eje que mejor se alinee
                        float dotMain = Mathf.Abs(Vector2.Dot(totalDelta.normalized, rotationDirection));
                        float dotAlt = Mathf.Abs(Vector2.Dot(totalDelta.normalized, alternativeDirection));

                        if (dotMain > dotAlt)
                        {
                            selectedAxis = rotationAxis;
                            selectedDirection = rotationDirection;
                            signedDistance = DistanceAlong(inputPosition, avgPosition, rotationDirection);
                        }
                        else
                        {
                            selectedAxis = alternativeAxis;
                            selectedDirection = alternativeDirection;
                            signedDistance = DistanceAlong(inputPosition, avgPosition, alternativeDirection);
                        }

                        startedRotation = true;
                        break;
                    }
                }
                else
                {
                    // Método original - calcular proyecciones a lo largo de ambos ejes
                    float distanceMain = DistanceAlong(inputPosition, avgPosition, rotationDirection);
                    float distanceAlt = DistanceAlong(inputPosition, avgPosition, alternativeDirection);

                    // Elegir el eje con mayor desplazamiento
                    if (Mathf.Abs(distanceMain) > Mathf.Abs(distanceAlt) && Mathf.Abs(distanceMain) > minimumDragPixels)
                    {
                        selectedAxis = rotationAxis;
                        selectedDirection = rotationDirection;
                        signedDistance = distanceMain;
                        startedRotation = true;
                        break;
                    }
                    else if (Mathf.Abs(distanceAlt) > minimumDragPixels)
                    {
                        selectedAxis = alternativeAxis;
                        selectedDirection = alternativeDirection;
                        signedDistance = distanceAlt;
                        startedRotation = true;
                        break;
                    }
                }
            }

            // Si ha pasado tiempo y no hay arrastre suficiente, cancelamos
            if (Time.time - waitStartTime > 0.3f) // Tiempo incrementado para móviles
            {
                yield break;
            }

            yield return null;
        }

        // Si no iniciamos rotación o el usuario soltó antes de arrastrar lo suficiente
        if (!startedRotation)
        {
            yield break;
        }

        // A partir de aquí tenemos un eje de rotación y dirección
        isRotatingFace = true;

        // Reunir los cubos que forman parte de la capa a rotar
        Vector3 extents = (Vector3.one - 0.9f * selectedAxis) * 3.0f; // Increased from 2.0f to 3.0f
        int totalCount = Physics.OverlapBoxNonAlloc(hit.collider.transform.position, extents, _subCubes, Quaternion.identity, 
                                                  Physics.AllLayers, QueryTriggerInteraction.Collide);

        // Filtrar para incluir solo cubies (no stickers)
        int subCubeCount = FilterCubies(totalCount);

        // Debug.Log("Encontrados " + totalCount + " colliders, usando " + subCubeCount + " cubies para rotación");

        // Determinar la dirección de rotación basada en el signo de la distancia
        float targetAngle = Mathf.Sign(signedDistance) * 90.0f;

        // Animación de rotación rápida a 90 grados
        float currentAngle = 0f;
        float rotationSpeed = snapSpeed;

        while (Mathf.Abs(currentAngle) < Mathf.Abs(targetAngle))
        {
            currentAngle += Mathf.Sign(targetAngle) * rotationSpeed * Time.deltaTime;

            // Asegurar que no sobrepasamos el ángulo objetivo
            if (Mathf.Abs(currentAngle) > Mathf.Abs(targetAngle))
                currentAngle = targetAngle;

            RotateGroup(currentAngle, selectedAxis, subCubeCount);
            yield return null;
        }

        // Finalizar rotación con precisión
        RotateGroup(targetAngle, selectedAxis, subCubeCount);

        // Restaurar estados
        isRotatingFace = false;

        // Determine which face was rotated based on the normal axis and rotation axis
        char face = DetermineFaceFromAxis(selectedAxis);
        bool clockwise = DetermineRotationDirection(selectedAxis, signedDistance);

        // Trigger the event to notify that a manual rotation is completed
        OnManualRotationCompleted?.Invoke(face, clockwise);
    }

    // Convierte la dirección en el mundo a dirección en pantalla a partir de un punto base.
    Vector2 ScreenDirection(Vector2 screenPoint, Vector3 worldPoint, Vector3 worldDirection)
    {
        Vector2 shifted = _camera.WorldToScreenPoint(worldPoint + worldDirection);
        return (shifted - screenPoint).normalized;
    }

    // Calcula la distancia a lo largo de una dirección dada.
    float DistanceAlong(Vector2 start, Vector2 current, Vector2 direction)
    {
        return Vector2.Dot(current - start, direction);
    }

    // Aplica la rotación calculada a los subcubos.
    void RotateGroup(float angle, Vector3 axis, int count)
    {
        Quaternion rotation = Quaternion.AngleAxis(angle, axis);
        for (int i = 0; i < count; i++)
        {
            var subCube = _subCubes[i].transform;

            // Obtener el componente Cubito para verificar si es un cubie principal
            Cubito cubito = subCube.GetComponent<Cubito>();

            if (cubito != null)
            {
                // Es un cubie principal, aplicar rotación directamente
                subCube.position = rotation * (_originalPositions[i] - transform.position) + transform.position;
                subCube.rotation = rotation * _originalOrientations[i];
            }
        }
    }

    // Método público para rotar una cara programáticamente
    public IEnumerator RotateFaceProgrammatically(char face, bool clockwise)
    {
        // No podemos rotar si ya estamos rotando
        if (isRotatingFace)
        {
            yield break;
        }

        isRotatingFace = true;

        // Determinar dirección normal de la cara
        Vector3 normal = GetFaceNormal(face);

        // Crear un punto del que lanzar un rayo para simular un toque en esa cara
        Vector3 rayOrigin = transform.position + normal * 5.0f;
        Ray ray = new Ray(rayOrigin, -normal);

        // Lanzar el rayo para encontrar una pieza de la cara
        RaycastHit hit;
        if (!Physics.Raycast(ray, out hit))
        {
            isRotatingFace = false;
            yield break;
        }

        // Determinar el eje de rotación basado en la cara
        Vector3 rotationAxis = GetRotationAxis(face, clockwise);

        // Reunir los cubos que forman parte de la capa a rotar
        Vector3 extents = new Vector3(
            face == 'L' || face == 'R' ? 0.2f : 3.0f,
            face == 'U' || face == 'D' ? 0.2f : 3.0f,
            face == 'F' || face == 'B' ? 0.2f : 3.0f
        );

        int totalCount = Physics.OverlapBoxNonAlloc(hit.collider.transform.position, extents, _subCubes, Quaternion.identity, 
                                                  Physics.AllLayers, QueryTriggerInteraction.Collide);

        // Filtrar para incluir solo cubies (no stickers)
        int subCubeCount = FilterCubies(totalCount);

        // Si no encontramos suficientes cubitos (debe ser 9 para un cubo 3x3)
        if (subCubeCount < 9)
        {
            // Intento alternativo con una caja más grande
            extents = new Vector3(
                face == 'L' || face == 'R' ? 0.5f : 3.0f,
                face == 'U' || face == 'D' ? 0.5f : 3.0f,
                face == 'F' || face == 'B' ? 0.5f : 3.0f
            );

            totalCount = Physics.OverlapBoxNonAlloc(hit.collider.transform.position, extents, _subCubes, Quaternion.identity, 
                                                  Physics.AllLayers, QueryTriggerInteraction.Collide);
            subCubeCount = FilterCubies(totalCount);

            // Si aún no encontramos 9 piezas, intentar con una caja aún más grande
            if (subCubeCount < 9)
            {
                extents = new Vector3(
                    face == 'L' || face == 'R' ? 1.0f : 4.0f,
                    face == 'U' || face == 'D' ? 1.0f : 4.0f,
                    face == 'F' || face == 'B' ? 1.0f : 4.0f
                );

                totalCount = Physics.OverlapBoxNonAlloc(hit.collider.transform.position, extents, _subCubes, Quaternion.identity, 
                                                      Physics.AllLayers, QueryTriggerInteraction.Collide);
                subCubeCount = FilterCubies(totalCount);
            }
        }

        // Animación de rotación
        float targetAngle = 90.0f;
        float currentAngle = 0f;
        float rotationSpeed = snapSpeed;

        while (currentAngle < targetAngle)
        {
            currentAngle += rotationSpeed * Time.deltaTime;

            // Asegurar que no sobrepasamos el ángulo objetivo
            if (currentAngle > targetAngle)
                currentAngle = targetAngle;

            // Aplicar rotación 
            RotateGroup(currentAngle, rotationAxis, subCubeCount);
            yield return null;
        }

        // Finalizar rotación con precisión
        RotateGroup(targetAngle, rotationAxis, subCubeCount);

        // Esperar varios frames para asegurarnos de que la rotación se aplicó y la física se estabilizó
        for (int i = 0; i < 5; i++)
        {
            yield return null;
        }

        // Esperar un tiempo adicional para asegurar que todo se estabilizó
        yield return new WaitForSeconds(0.1f);

        // Restaurar estado
        isRotatingFace = false;

        // Trigger the event to notify that a programmatic rotation is completed
        OnManualRotationCompleted?.Invoke(face, clockwise);
    }

    // Método auxiliar para obtener la normal de una cara
    private Vector3 GetFaceNormal(char face)
    {
        switch (face)
        {
            case 'U': return Vector3.up;
            case 'D': return Vector3.down;
            case 'R': return Vector3.left;  // Swapped: was Vector3.right
            case 'L': return Vector3.right; // Swapped: was Vector3.left
            case 'F': return Vector3.forward;
            case 'B': return Vector3.back;
            default: return Vector3.zero;
        }
    }

    // Método auxiliar para obtener el eje de rotación según la cara y dirección
    private Vector3 GetRotationAxis(char face, bool clockwise)
    {
        Vector3 axis;

        // Determine the base axis for each face
        switch (face)
        {
            case 'U': axis = Vector3.up; break;
            case 'D': axis = Vector3.down; break;
            case 'R': axis = Vector3.left; break;  // Swapped: was Vector3.right
            case 'L': axis = Vector3.right; break; // Swapped: was Vector3.left
            case 'F': axis = Vector3.forward; break;
            case 'B': axis = Vector3.back; break;
            default: axis = Vector3.zero; break;
        }

        // Apply a consistent rule for all faces:
        // - For clockwise rotations, use the axis as is
        // - For counterclockwise rotations, invert the axis
        if (!clockwise)
        {
            axis = -axis;
        }

        return axis;
    }

    // Método para determinar la cara a partir del eje de rotación
    private char DetermineFaceFromAxis(Vector3 axis)
    {
        // Normalizar el eje para comparaciones más precisas
        axis = axis.normalized;

        if (Mathf.Approximately(axis.y, 1f) || Mathf.Approximately(axis.y, -1f))
        {
            return axis.y > 0 ? 'U' : 'D';
        }
        else if (Mathf.Approximately(axis.x, 1f) || Mathf.Approximately(axis.x, -1f))
        {
            return axis.x > 0 ? 'L' : 'R';  // Swapped: was R : L
        }
        else if (Mathf.Approximately(axis.z, 1f) || Mathf.Approximately(axis.z, -1f))
        {
            return axis.z > 0 ? 'F' : 'B';
        }

        // Si no podemos determinar la cara, devolvemos un valor por defecto
        // Debug.LogWarning("No se pudo determinar la cara a partir del eje: " + axis);
        return 'U';
    }

    // Método para determinar la dirección de rotación
    private bool DetermineRotationDirection(Vector3 axis, float signedDistance)
    {
        // Normalizar el eje para comparaciones más precisas
        axis = axis.normalized;

        // La dirección de rotación depende del eje y del signo de la distancia
        if (axis.y > 0) // U
        {
            return signedDistance > 0;
        }
        else if (axis.y < 0) // D
        {
            return signedDistance < 0;
        }
        else if (axis.x > 0) // L (Swapped: was R)
        {
            return signedDistance > 0;
        }
        else if (axis.x < 0) // R (Swapped: was L)
        {
            return signedDistance < 0;
        }
        else if (axis.z > 0) // F
        {
            return signedDistance > 0;
        }
        else if (axis.z < 0) // B
        {
            return signedDistance < 0;
        }

        // Si no podemos determinar la dirección, devolvemos un valor por defecto
        // Debug.LogWarning("No se pudo determinar la dirección de rotación para el eje: " + axis);
        return true;
    }

    // Método para resetear el cubo completamente
    public void ResetCubeCompletely()
    {
        // Detener cualquier rotación en progreso
        isRotatingFace = false;

        // Buscar todos los cubitos y resetearlos a su posición original
        Cubito[] cubitos = GetComponentsInChildren<Cubito>();

        if (cubitos.Length == 0)
        {
            // Debug.LogError("No se encontraron componentes Cubito en los hijos. Añadiendo componentes...");
            AddCubitoComponentToAllPieces();
            cubitos = GetComponentsInChildren<Cubito>();

            if (cubitos.Length == 0)
            {
                // Debug.LogError("Sigue sin encontrar cubitos después de añadir componentes!");
                return;
            }
        }

        // Restablecer cada pieza a su posición inicial
        foreach (Cubito cubito in cubitos)
        {
            cubito.transform.localPosition = cubito.posicionInicial;
            cubito.transform.localRotation = cubito.rotacionInicial;
        }

        // Debug.Log("Cubo restablecido a su posición inicial. Encontrados " + cubitos.Length + " cubitos.");
    }

    // Método para filtrar los cubies (sin incluir los stickers)
    private int FilterCubies(int totalCount)
    {
        // Primero, intentamos usar los colliders encontrados por Physics.OverlapBoxNonAlloc
        int subCubeCount = 0;

        // Filtrar para incluir solo los cubies (no los stickers)
        for (int i = 0; i < totalCount; i++)
        {
            // Verificar si es un cubie (no un sticker)
            if (_subCubes[i].transform.GetComponent<Cubito>() != null || 
                _subCubes[i].transform.parent != null && _subCubes[i].transform.parent.GetComponent<Cubito>() != null)
            {
                // Si es un cubie, incluirlo
                if (i != subCubeCount)
                {
                    _subCubes[subCubeCount] = _subCubes[i];
                }

                var subCube = _subCubes[subCubeCount].transform;
                _originalPositions[subCubeCount] = subCube.position;
                _originalOrientations[subCubeCount] = subCube.rotation;

                subCubeCount++;
            }
        }

        // Si no encontramos suficientes cubies (debe ser 9 para un cubo 3x3), 
        // usamos GetComponentsInChildren para obtener todos los cubies directamente
        if (subCubeCount < 9)
        {
            // Obtener todos los cubies
            Cubito[] cubitos = GetComponentsInChildren<Cubito>(true); // true para incluir objetos inactivos

            // Reiniciar el contador
            subCubeCount = 0;

            // Agregar todos los cubitos encontrados
            foreach (Cubito cubito in cubitos)
            {
                if (subCubeCount < _subCubes.Length)
                {
                    // Obtener el collider del cubito
                    Collider collider = cubito.GetComponent<Collider>();
                    if (collider != null)
                    {
                        _subCubes[subCubeCount] = collider;
                        _originalPositions[subCubeCount] = cubito.transform.position;
                        _originalOrientations[subCubeCount] = cubito.transform.rotation;
                        subCubeCount++;
                    }
                }
                else
                {
                    break;
                }
            }
        }

        return subCubeCount;
    }

    // Método para verificar si las posiciones de los cubitos son válidas
    public bool VerificarPosicionesCubitos()
    {
        Cubito[] cubitos = GetComponentsInChildren<Cubito>();
        Vector3[] posiciones = new Vector3[cubitos.Length];

        for (int i = 0; i < cubitos.Length; i++)
        {
            Vector3 posicionRedondeada = new Vector3(
                Mathf.Round(cubitos[i].transform.localPosition.x),
                Mathf.Round(cubitos[i].transform.localPosition.y),
                Mathf.Round(cubitos[i].transform.localPosition.z)
            );

            // Comprobar colisiones (piezas superpuestas)
            for (int j = 0; j < i; j++)
            {
                if (Vector3.Distance(posicionRedondeada, posiciones[j]) < 0.1f)
                {
                    // Debug.LogError("¡Detectadas piezas superpuestas! Posición: " + posicionRedondeada);
                    return false;
                }
            }

            posiciones[i] = posicionRedondeada;
        }

        // Debug.Log("Verificación completada: No hay piezas superpuestas.");
        return true;
    }

    // Rotation methods for ImprovedCubeSolver

    /// <summary>
    /// Rotates the Up face of the cube
    /// </summary>
    /// <param name="clockwise">If true, rotates clockwise; otherwise, counterclockwise</param>
    /// <param name="double_move">If true, performs a 180-degree rotation</param>
    public void RotateUp(bool clockwise, bool double_move)
    {
        StartCoroutine(RotateUpCoroutine(clockwise, double_move));
    }

    private IEnumerator RotateUpCoroutine(bool clockwise, bool double_move)
    {
        yield return StartCoroutine(RotateFaceProgrammatically('U', clockwise));

        // If it's a double move, rotate again after the first rotation completes
        if (double_move)
        {
            yield return StartCoroutine(RotateFaceProgrammatically('U', clockwise));
        }
    }

    /// <summary>
    /// Rotates the Down face of the cube
    /// </summary>
    /// <param name="clockwise">If true, rotates clockwise; otherwise, counterclockwise</param>
    /// <param name="double_move">If true, performs a 180-degree rotation</param>
    public void RotateDown(bool clockwise, bool double_move)
    {
        StartCoroutine(RotateDownCoroutine(clockwise, double_move));
    }

    private IEnumerator RotateDownCoroutine(bool clockwise, bool double_move)
    {
        yield return StartCoroutine(RotateFaceProgrammatically('D', clockwise));

        // If it's a double move, rotate again after the first rotation completes
        if (double_move)
        {
            yield return StartCoroutine(RotateFaceProgrammatically('D', clockwise));
        }
    }

    /// <summary>
    /// Rotates the Right face of the cube
    /// </summary>
    /// <param name="clockwise">If true, rotates clockwise; otherwise, counterclockwise</param>
    /// <param name="double_move">If true, performs a 180-degree rotation</param>
    public void RotateRight(bool clockwise, bool double_move)
    {
        StartCoroutine(RotateRightCoroutine(clockwise, double_move));
    }

    private IEnumerator RotateRightCoroutine(bool clockwise, bool double_move)
    {
        yield return StartCoroutine(RotateFaceProgrammatically('R', clockwise));

        // If it's a double move, rotate again after the first rotation completes
        if (double_move)
        {
            yield return StartCoroutine(RotateFaceProgrammatically('R', clockwise));
        }
    }

    /// <summary>
    /// Rotates the Left face of the cube
    /// </summary>
    /// <param name="clockwise">If true, rotates clockwise; otherwise, counterclockwise</param>
    /// <param name="double_move">If true, performs a 180-degree rotation</param>
    public void RotateLeft(bool clockwise, bool double_move)
    {
        StartCoroutine(RotateLeftCoroutine(clockwise, double_move));
    }

    private IEnumerator RotateLeftCoroutine(bool clockwise, bool double_move)
    {
        yield return StartCoroutine(RotateFaceProgrammatically('L', clockwise));

        // If it's a double move, rotate again after the first rotation completes
        if (double_move)
        {
            yield return StartCoroutine(RotateFaceProgrammatically('L', clockwise));
        }
    }

    /// <summary>
    /// Rotates the Front face of the cube
    /// </summary>
    /// <param name="clockwise">If true, rotates clockwise; otherwise, counterclockwise</param>
    /// <param name="double_move">If true, performs a 180-degree rotation</param>
    public void RotateFront(bool clockwise, bool double_move)
    {
        StartCoroutine(RotateFrontCoroutine(clockwise, double_move));
    }

    private IEnumerator RotateFrontCoroutine(bool clockwise, bool double_move)
    {
        yield return StartCoroutine(RotateFaceProgrammatically('F', clockwise));

        // If it's a double move, rotate again after the first rotation completes
        if (double_move)
        {
            yield return StartCoroutine(RotateFaceProgrammatically('F', clockwise));
        }
    }

    /// <summary>
    /// Rotates the Back face of the cube
    /// </summary>
    /// <param name="clockwise">If true, rotates clockwise; otherwise, counterclockwise</param>
    /// <param name="double_move">If true, performs a 180-degree rotation</param>
    public void RotateBack(bool clockwise, bool double_move)
    {
        StartCoroutine(RotateBackCoroutine(clockwise, double_move));
    }

    private IEnumerator RotateBackCoroutine(bool clockwise, bool double_move)
    {
        yield return StartCoroutine(RotateFaceProgrammatically('B', clockwise));

        // If it's a double move, rotate again after the first rotation completes
        if (double_move)
        {
            yield return StartCoroutine(RotateFaceProgrammatically('B', clockwise));
        }
    }


}
