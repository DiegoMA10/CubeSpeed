const { onDocumentWritten, onDocumentDeleted } = require("firebase-functions/v2/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const admin = require('firebase-admin');
const { AggregateField } = require('firebase-admin/firestore');

// Initialize Firebase Admin SDK
admin.initializeApp();

/**
 * Gets aggregated statistics for a user, cube type, and tag
 * @param {string} userId - The user ID
 * @param {string} cubeType - The cube type
 * @param {string} tagId - The tag ID
 * @returns {Object} - The aggregated statistics
 */
async function getStatsAggregated(userId, cubeType, tagId) {
  const db = admin.firestore();
  // Get a reference to the collection
  const collectionRef = db.collection("users").doc(userId).collection("timers");
  const query = collectionRef.where("cube", "==", cubeType).where("tagId", "==", tagId);

  // 1) COUNT - Using count() aggregation
  let totalSolves = 0;
  try {
    const countSnapshot = await query.count().get();
    totalSolves = countSnapshot.data().count;
  } catch (e) {
    console.error(`Error calculating count: ${e}`);
  }

  // 2) AVG - Using average() aggregation
  let avgDuration = 0;
  try {
    const averageAggregateQuery = query.aggregate({
      averageDuration: AggregateField.average("duration")
    });

    const averageAggregateSnapshot = await averageAggregateQuery.get();
    avgDuration = averageAggregateSnapshot.data().averageDuration || 0;
  } catch (e) {
    console.error(`Error calculating average: ${e}`);
    // Fallback to manual calculation if aggregation fails
    try {
      const querySnapshot = await query.get();
      if (!querySnapshot.empty) {
        let sum = 0;
        let count = 0;
        querySnapshot.forEach(doc => {
          const data = doc.data();
          if (data.duration && typeof data.duration === 'number') {
            sum += data.duration;
            count++;
          }
        });
        avgDuration = count > 0 ? sum / count : 0.0;
      }
    } catch (e) {
      console.error(`Error calculating average manually: ${e}`);
    }
  }

  // 3) BEST - Get the best document (lowest duration)
  let best = null;
  try {
    const bestDocs = await query.orderBy("duration", "asc").limit(1).get();
    if (!bestDocs.empty) {
      best = bestDocs.docs[0].data();
    }
  } catch (e) {
    console.error(`Error getting best solve: ${e}`);
  }

  return { count: totalSolves, average: avgDuration, best: best };
}

/**
 * Calculates the average of N times, excluding the best and worst times.
 * @param {Array<number>} times - Array of times
 * @param {number} n - Number of times to consider
 * @returns {number} - The average of N times
 */
function calculateAverageOfN(times, n) {
  if (times.length < n) {
    return 0;
  }

  // Get the most recent n times
  const recentTimes = times.slice(0, n);

  if (n <= 2) {
    // For n <= 2, just return the average
    return recentTimes.reduce((a, b) => a + b, 0) / recentTimes.length;
  }

  // For n > 2, exclude best and worst
  const sortedTimes = [...recentTimes].sort((a, b) => a - b);
  const trimmedTimes = sortedTimes.slice(1, -1); // Remove best and worst
  return trimmedTimes.reduce((a, b) => a + b, 0) / trimmedTimes.length;
}

/**
 * Recalculates statistics for a user, cube type, and tag
 * @param {string} userId - The user ID
 * @param {string} cubeType - The cube type
 * @param {string} tagId - The tag ID
 */
async function recalculateStats(userId, cubeType, tagId) {
  console.log(`Recalculating stats for user ${userId}, cube ${cubeType}, tag ${tagId}`);

  const db = admin.firestore();
  const statsRef = db.collection("users").doc(userId).collection("stats");
  const statsDocId = `${cubeType}_${tagId}`;

  // Define the reference to the timers collection
  const baseQ = db.collection("users")
    .doc(userId)
    .collection("timers")
    .where("cube", "==", cubeType)
    .where("tagId", "==", tagId);

  // Use getStatsAggregated to get basic statistics
  const aggregatedStats = await getStatsAggregated(userId, cubeType, tagId);

  // Get current statistics for additional fields
  const statsDoc = await statsRef.doc(statsDocId).get();
  const currentStats = statsDoc.exists ? statsDoc.data() : {};

  // Use values from aggregated statistics and ensure they are numbers
  const count = parseInt(aggregatedStats.count) || 0;
  const average = parseFloat(aggregatedStats.average) || 0;
  const best = aggregatedStats.best;
  let deviation = 0;

  // Calculate valid_count by counting solves that are not DNF
  let numValidSolves = 0;
  try {
    const validCountQuery = db.collection("users").doc(userId).collection("timers")
      .where("cube", "==", cubeType)
      .where("tagId", "==", tagId)
      .where("status", "!=", "DNF");

    const validCountSnapshot = await validCountQuery.count().get();
    numValidSolves = validCountSnapshot.data().count;
  } catch (e) {
    // If aggregation fails, use current value or estimate
    console.error(`[recalculateStats] Error calculating valid_count: ${e}`);
    numValidSolves = parseInt(currentStats.validCount) || 0;
    if (!numValidSolves && count > 0) {
      numValidSolves = count; // Estimation
    }
  }

  // Calculate sum_time for incremental statistics
  let sumTime = 0;
  if (average > 0 && numValidSolves > 0) {
    sumTime = average * numValidSolves;
  }

  // For AoN (moving averages) only read the last 100 solves
  let solves = [];
  try {
    const solvesQuery = baseQ
      .orderBy("timestamp", "desc")
      .limit(100);

    const solvesSnapshot = await solvesQuery.get();
    solvesSnapshot.forEach(doc => {
      const solve = doc.data();
      if (solve) {
        solves.push(solve);
      }
    });
  } catch (e) {
    // Index still BUILDING, exit without interrupting everything
    console.error(`[recalculateStats] Index still building: ${e}`);
    return;
  }

  const validSolves = solves.filter(s => s.status !== "DNF");
  const times = validSolves.map(s => parseFloat(s.duration) || 0);

  // Calculate standard deviation
  deviation = 0;
  if (times.length >= 2) { // Standard deviation requires at least 2 values
    try {
      // Calculate standard deviation manually
      const mean = times.reduce((a, b) => a + b, 0) / times.length;
      const squareDiffs = times.map(value => {
        const diff = value - mean;
        return diff * diff;
      });
      const avgSquareDiff = squareDiffs.reduce((a, b) => a + b, 0) / squareDiffs.length;
      deviation = Math.sqrt(avgSquareDiff);
    } catch (e) {
      console.error(`[recalculateStats] Error calculating standard deviation: ${e}`);
      deviation = 0;
    }
  }

  // Create statistics object
  const stats = {
    count: count,
    validCount: numValidSolves,
    sum: sumTime,
    best: best ? best.duration : 0,
    average: average,
    deviation: deviation,
    ao5: calculateAverageOfN(times, 5),
    ao12: calculateAverageOfN(times, 12),
    ao50: calculateAverageOfN(times, 50),
    ao100: calculateAverageOfN(times, 100)
  };

  // Save statistics
  await statsRef.doc(statsDocId).set(stats);
}

// Firestore trigger function that runs when a solve is added or updated
exports.updateStatsOnSolve = onDocumentWritten(
  { 
    document: 'users/{userId}/timers/{timerId}',
    region: "us-central1",
    memory: "1GiB",     // Less memory than getStats as it's a background operation
    cpu: 2,               // Single CPU is sufficient for this operation
    maxInstances: 10      // Limit concurrent executions
  },
  async (event) => {
    const userId = event.params.userId;

    // Handle document deletion - this is now handled by updateStatsOnDelete
    if (!event.data.after) {
      return null;
    }

    // Handle document creation or update
    const solveData = event.data.after.data();
    if (!solveData) {
      return null;
    }

    const cubeType = solveData.cube;
    const tagId = solveData.tagId;
    if (!cubeType || !tagId) {
      return null;
    }

    // For creation or update, recalculate stats
    await recalculateStats(userId, cubeType, tagId);
    return null;
  });

// Firestore trigger function that runs when a solve is deleted
exports.updateStatsOnDelete = onDocumentDeleted(
  { 
    document: 'users/{userId}/timers/{timerId}',
    region: "us-central1",
    memory: "1GiB",     // Same configuration as updateStatsOnSolve
    cpu: 2,               // Single CPU is sufficient for this operation
    maxInstances: 10      // Limit concurrent executions
  },
  async (event) => {
    const userId = event.params.userId;

    // Get data from the snapshot (which contains the document before deletion)
    const solveData = event.data.data();
    if (!solveData) {
      return null;
    }

    const cubeType = solveData.cube;
    const tagId = solveData.tagId;
    if (!cubeType || !tagId) {
      return null;
    }

    console.log(`Document deleted. Recalculating stats for user ${userId}, cube ${cubeType}, tag ${tagId}`);
    // Recalculate stats after deletion
    await recalculateStats(userId, cubeType, tagId);
    return null;
  });

// HTTP function to get statistics for a user
exports.getStats = onRequest(
  {
    region: "us-central1",
    memory: "1GiB",       // Optional: allowed values "256MiB", "512MiB", "1GiB", etc.
    cpu: 1,               // Optional: values 1, 2, 4, depending on how much RAM you use
   // minInstances: 1,      // Optional: always active instance to avoid cold start
    maxInstances: 10      // Optional: to limit autoscaling
  },
  async (req, res) => {
  // Get query parameters
  const userId = req.query.userId;
  const cubeType = req.query.cubeType;
  const tagId = req.query.tagId;

  if (!userId || !cubeType || !tagId) {
    return res.status(400).json({
      error: "Missing required parameters"
    });
  }

  // Get statistics from Firestore
  const db = admin.firestore();
  const statsRef = db.collection("users").doc(userId).collection("stats");
  const statsDocId = `${cubeType}_${tagId}`;
  const statsDoc = await statsRef.doc(statsDocId).get();

  if (!statsDoc.exists) {
    // Return empty statistics if not found
    return res.json({
      count: 0,
      validCount: 0,
      best: 0,
      average: 0,
      deviation: 0,
      ao5: 0,
      ao12: 0,
      ao50: 0,
      ao100: 0
    });
  }

  // Return statistics
  return res.json(statsDoc.data());
});
