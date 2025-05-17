const {onDocumentWritten, onDocumentDeleted} = require("firebase-functions/v2/firestore");
const {onRequest} = require("firebase-functions/v2/https");
const admin = require('firebase-admin');
const {AggregateField} = require('firebase-admin/firestore');
const {FieldValue} = require('firebase-admin/firestore');

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
    const collectionRef = db.collection("users").doc(userId).collection("solves");
    const query = collectionRef.where("cube", "==", cubeType).where("tagId", "==", tagId);

    let totalSolves = 0;
    let avgDuration = 0;
    let best = null;

    try {
        const countSnapshot = await query.count().get();
        totalSolves = countSnapshot.data().count;
    } catch (e) {
        console.error(`Error calculating count: ${e}`);
    }

    try {
        // Only include non-DNF solves in average calculation
        const validQuery = query.where("status", "!=", "DNF");
        const averageAggregateQuery = validQuery.aggregate({
            averageDuration: AggregateField.average("duration")
        });

        const averageAggregateSnapshot = await averageAggregateQuery.get();
        avgDuration = averageAggregateSnapshot.data().averageDuration || 0;
    } catch (e) {
        console.error(`Error calculating average: ${e}`);
    }

    try {
        // Only include non-DNF solves when determining best time
        const validQuery = query.where("status", "!=", "DNF");
        const bestDocs = await validQuery.orderBy("status", "asc").orderBy("duration", "asc").limit(1).get();
        if (!bestDocs.empty) {
            best = bestDocs.docs[0].data();
        }
    } catch (e) {
        console.error(`Error getting best solve: ${e}`);
    }

    return {count: totalSolves, average: avgDuration, best: best};
}

/**
 * Calculates the average of N times, excluding the best and worst times.
 * Handles DNFs according to the specified rules:
 * - Ao5, Ao12: Allow 1 DNF (if it's removed as worst time), otherwise return DNF
 * - Ao50: Allow up to 2 DNFs (if they're removed), otherwise return DNF
 * - Ao100: Allow up to 5 DNFs (if they're removed), otherwise return DNF
 *
 * @param {Array<number>} times - Array of times
 * @param {Array<Object>} solves - Array of solve objects with status information
 * @param {number} n - Number of times to consider
 * @returns {number} - The average of N times, or -1 for DNF
 */
function calculateAverageOfN(times, n, solves = []) {
    if (times.length < n) {
        return 0;
    }

    // Get the n most recent times and solves
    const recentTimes = times.slice(0, n);
    const recentSolves = solves.slice(0, n);

    if (n <= 2) {
        return calculateSimpleAverage(recentTimes);
    }

    // Count DNFs
    const dnfCount = recentSolves.filter(solve => solve && solve.status === "DNF").length;

    // Determine max allowed DNFs and number of times to trim based on n
    let maxAllowedDNFs, trimCount;
    if (n <= 12) {
        // Ao5, Ao12: Remove 1 best and 1 worst, allow 1 DNF
        maxAllowedDNFs = 1;
        trimCount = 1;
    } else if (n <= 50) {
        // Ao50: Remove 5% (2 best and 2 worst), allow 2 DNFs
        maxAllowedDNFs = 2;
        trimCount = 2;
    } else {
        // Ao100: Remove 5% (5 best and 5 worst), allow 5 DNFs
        maxAllowedDNFs = 5;
        trimCount = 5;
    }

    // If there are too many DNFs, the average is DNF
    if (dnfCount > maxAllowedDNFs) {
        return -1; // Use -1 to represent DNF
    }

    // Create pairs of [time, isDNF] for sorting
    const timePairs = recentTimes.map((time, i) => {
        const isDNF = recentSolves[i] && recentSolves[i].status === "DNF";
        return [time, isDNF];
    });

    // Sort pairs: DNFs are considered worst times
    const sortedPairs = [...timePairs].sort((a, b) => {
        // If one is DNF and the other is not, DNF is worse
        if (a[1] && !b[1]) return 1;
        if (!a[1] && b[1]) return -1;
        // Otherwise, compare times
        return a[0] - b[0];
    });

    // Remove trimCount best and trimCount worst times
    const trimmedPairs = sortedPairs.slice(trimCount, sortedPairs.length - trimCount);

    // If any remaining time is DNF, the average is DNF
    if (trimmedPairs.some(pair => pair[1])) {
        return -1; // DNF
    }

    // Extract times from pairs and calculate average
    const trimmedTimes = trimmedPairs.map(pair => pair[0]);
    return calculateSimpleAverage(trimmedTimes);
}

/**
 * Calculates a simple average of an array of numbers
 * @param {Array<number>} numbers - Array of numbers
 * @returns {number} - The average
 */
function calculateSimpleAverage(numbers) {
    if (!numbers.length) return 0;
    return numbers.reduce((sum, value) => sum + value, 0) / numbers.length;
}

/**
 * Calculates the standard deviation of an array of numbers
 * @param {Array<number>} numbers - Array of numbers
 * @returns {number} - The standard deviation
 */
function calculateStandardDeviation(numbers) {
    if (numbers.length < 2) return 0; // Standard deviation requires at least 2 values

    try {
        const mean = calculateSimpleAverage(numbers);
        const squareDiffs = numbers.map(value => {
            const diff = value - mean;
            return diff * diff;
        });
        const avgSquareDiff = calculateSimpleAverage(squareDiffs);
        return Math.sqrt(avgSquareDiff);
    } catch (e) {
        console.error(`Error calculating standard deviation: ${e}`);
        return 0;
    }
}

/**
 * Optimizes the recentSolves array to only include necessary fields
 * @param {Array} solves - Array of solves
 * @param {string} cubeType - The cube type
 * @param {string} tagId - The tag ID
 * @returns {Array} - Optimized array of solves
 */
function optimizeRecentSolves(solves, cubeType, tagId) {
    return solves.map(solve => ({
        id: solve.id,
        duration: parseFloat(solve.duration) || 0,
        status: solve.status,
        timestamp: solve.timestamp,
        cube: solve.cube || cubeType,  // Ensure cube type is set
        tagId: solve.tagId || tagId    // Ensure tag ID is set
    }));
}

/**
 * Gets the count of valid solves (not DNF) for a user, cube type, and tag
 * @param {object} db - Firestore database instance
 * @param {string} userId - The user ID
 * @param {string} cubeType - The cube type
 * @param {string} tagId - The tag ID
 * @param {number} count - Total count of solves
 * @param {object} currentStats - Current statistics
 * @returns {number} - The count of valid solves
 */
async function getValidSolvesCount(db, userId, cubeType, tagId, count, currentStats) {
    try {
        const validCountQuery = db.collection("users").doc(userId).collection("solves")
            .where("cube", "==", cubeType)
            .where("tagId", "==", tagId)
            .where("status", "!=", "DNF");

        const validCountSnapshot = await validCountQuery.count().get();
        return validCountSnapshot.data().count;
    } catch (e) {
        console.error(`Error calculating valid_count: ${e}`);
        const currentValidCount = parseInt(currentStats.validCount) || 0;
        return currentValidCount || (count > 0 ? count : 0); // Use current value or estimate
    }
}

/**
 * Updates the recentSolves array based on the operation (add, update, delete)
 * @param {object} db - Firestore database instance
 * @param {string} userId - The user ID
 * @param {string} cubeType - The cube type
 * @param {string} tagId - The tag ID
 * @param {Array} currentRecentSolves - Current recentSolves array
 * @param {Object} newSolve - The new solve data
 * @param {boolean} isDelete - Whether this is a delete operation
 * @param {number} totalCount - Total count of solves
 * @returns {Array} - Updated recentSolves array
 */
async function updateRecentSolves(db, userId, cubeType, tagId, currentRecentSolves, newSolve, isDelete, totalCount) {
    let recentSolves = [...currentRecentSolves];

    if (newSolve && !isDelete) {
        // Check if the solve already exists in the array
        const existingIndex = recentSolves.findIndex(solve => solve.id === newSolve.id);

        if (existingIndex >= 0) {
            // If it exists, update it in place to maintain its position
            recentSolves[existingIndex] = newSolve;
        } else {
            // If it's a new solve, add it to the beginning
            recentSolves.unshift(newSolve);

            // Limit to 100 solves
            if (recentSolves.length > 100) {
                recentSolves = recentSolves.slice(0, 100);
            }
        }
    } else if (newSolve && isDelete) {
        // For delete: Remove the solve from the array
        recentSolves = recentSolves.filter(solve => solve.id !== newSolve.id);

        // Fetch one more solve if needed to maintain up to 100 solves
        if (recentSolves.length < 100 && recentSolves.length < totalCount) {
            try {
                const oldestTimestamp = recentSolves.length > 0
                    ? recentSolves[recentSolves.length - 1].timestamp
                    : new Date().toISOString();

                const additionalSolveQuery = db.collection("users")
                    .doc(userId)
                    .collection("solves")
                    .where("cube", "==", cubeType)
                    .where("tagId", "==", tagId)
                    .orderBy("timestamp", "desc")
                    .startAfter(oldestTimestamp)
                    .limit(1);

                const additionalSolveSnapshot = await additionalSolveQuery.get();
                if (!additionalSolveSnapshot.empty) {
                    const additionalSolve = additionalSolveSnapshot.docs[0].data();
                    additionalSolve.id = additionalSolveSnapshot.docs[0].id;
                    recentSolves.push(additionalSolve);
                }
            } catch (e) {
                console.error(`Error fetching additional solve: ${e}`);
            }
        }
    }

    // Initialize recentSolves if empty but solves exist
    if (recentSolves.length === 0 && totalCount > 0) {
        try {
            const solvesQuery = db.collection("users")
                .doc(userId)
                .collection("solves")
                .where("cube", "==", cubeType)
                .where("tagId", "==", tagId)
                .orderBy("timestamp", "desc")
                .limit(100);

            const solvesSnapshot = await solvesQuery.get();
            solvesSnapshot.forEach(doc => {
                const solve = doc.data();
                if (solve) {
                    solve.id = doc.id;
                    recentSolves.push(solve);
                }
            });
        } catch (e) {
            console.error(`Error initializing recentSolves: ${e}`);
        }
    }

    // Ensure all solves have an ID
    return recentSolves.map(solve => {
        if (!solve.id && solve.timestamp) {
            console.warn(`Solve missing ID, using timestamp as fallback`);
            return {...solve, id: solve.timestamp.toString()};
        }
        return solve;
    });
}

/**
 * Recalculates statistics for a user, cube type, and tag
 * @param {string} userId - The user ID
 * @param {string} cubeType - The cube type
 * @param {string} tagId - The tag ID
 * @param {Object} newSolve - The new solve data (if adding/updating)
 * @param {boolean} isDelete - Whether this is a delete operation
 */
async function recalculateStats(userId, cubeType, tagId, newSolve = null, isDelete = false) {
    console.log(`Recalculating stats for user ${userId}, cube ${cubeType}, tag ${tagId}`);

    try {
        const db = admin.firestore();
        const statsRef = db.collection("users").doc(userId).collection("stats");
        const statsDocId = `${cubeType}_${tagId}`;
        const statsDocRef = statsRef.doc(statsDocId);

        // Get basic statistics
        const aggregatedStats = await getStatsAggregated(userId, cubeType, tagId);

        // Get current stats document
        const statsDoc = await statsDocRef.get();
        const currentStats = statsDoc.exists ? statsDoc.data() : {};

        // Parse and ensure values are numbers
        const count = parseInt(aggregatedStats.count) || 0;
        const average = parseFloat(aggregatedStats.average) || 0;
        const best = aggregatedStats.best;
        let deviation = 0;

        // Get count of valid solves
        const numValidSolves = await getValidSolvesCount(db, userId, cubeType, tagId, count, currentStats);

        // Calculate sum of times for incremental statistics
        let sumTime = 0;
        if (average > 0 && numValidSolves > 0) {
            sumTime = average * numValidSolves;
        }

        // Get or initialize the recentSolves array
        let recentSolves = currentStats.recentSolves || [];

        // Update recentSolves array based on operation
        recentSolves = await updateRecentSolves(db, userId, cubeType, tagId, recentSolves, newSolve, isDelete, count);

        // Extract times for calculations
        const times = recentSolves.map(s => parseFloat(s.duration) || 0);

        // Calculate standard deviation
        deviation = calculateStandardDeviation(times);

        // Optimize recentSolves to only include necessary fields
        const optimizedRecentSolves = optimizeRecentSolves(recentSolves, cubeType, tagId);

        // Create statistics object
        const stats = {
            count: count,
            validCount: numValidSolves,
            sum: sumTime,
            best: best ? best.duration : 0,
            average: average,
            deviation: deviation,
            recentSolves: optimizedRecentSolves,
            ao5: calculateAverageOfN(times, 5, recentSolves),
            ao12: calculateAverageOfN(times, 12, recentSolves),
            ao50: calculateAverageOfN(times, 50, recentSolves),
            ao100: calculateAverageOfN(times, 100, recentSolves),
            lastUpdated: FieldValue.serverTimestamp()
        };

        // Save statistics
        await statsDocRef.set(stats);
        console.log(`Stats updated for user ${userId}, cube ${cubeType}, tag ${tagId}`);
    } catch (error) {
        console.error(`Error updating stats for user ${userId}, cube ${cubeType}, tag ${tagId}: ${error}`);
    }
}

// Firestore trigger function that runs when a solve is added or updated
exports.updateStatsOnSolve = onDocumentWritten(
    {
        document: 'users/{userId}/solves/{solveId}',
        region: "us-central1",
        memory: "1GiB",
        cpu: 2,
        maxInstances: 10
    },
    async (event) => {
        const userId = event.params.userId;
        const solveId = event.params.solveId;

        // Skip if document was deleted (handled by updateStatsOnDelete)
        if (!event.data.after) {
            return null;
        }

        const solveData = event.data.after.data();
        if (!solveData) {
            return null;
        }

        const cubeType = solveData.cube;
        const tagId = solveData.tagId;
        if (!cubeType || !tagId) {
            return null;
        }

        // Add the document ID to the solve data
        const solveWithId = {...solveData, id: solveId};

        // If this is a new solve and timestamp is missing, add server timestamp
        if (!event.data.before && !solveData.timestamp) {
            // Get a reference to the solve document
            const db = admin.firestore();
            const solveRef = db.collection("users").doc(userId).collection("solves").doc(solveId);

            // Update the timestamp field with server timestamp
            await solveRef.update({
                timestamp: FieldValue.serverTimestamp()
            });

            // Update the solveWithId with the server timestamp
            solveWithId.timestamp = new Date();
        }

        await recalculateStats(userId, cubeType, tagId, solveWithId, false);
        return null;
    });

// Firestore trigger function that runs when a solve is deleted
exports.updateStatsOnDelete = onDocumentDeleted(
    {
        document: 'users/{userId}/solves/{solveId}',
        region: "us-central1",
        memory: "1GiB",
        cpu: 2,
        maxInstances: 10
    },
    async (event) => {
        const userId = event.params.userId;
        const solveId = event.params.solveId;

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

        // Add the document ID to the solve data
        const solveWithId = {...solveData, id: solveId};

        console.log(`Document deleted. Recalculating stats for user ${userId}, cube ${cubeType}, tag ${tagId}`);
        await recalculateStats(userId, cubeType, tagId, solveWithId, true);
        return null;
    });

/**
 * HTTP function to migrate solves from the old path to the new path
 * This function copies solves from /users/{userId}/timers/ to /users/{userId}/solves/
 */
exports.migrateSolves = onRequest(
    {
        region: "us-central1",
        memory: "1GiB",
        cpu: 1,
        maxInstances: 1
    },
    async (req, res) => {
        const userId = req.query.userId;

        if (!userId) {
            return res.status(400).json({
                error: "Missing required parameter: userId"
            });
        }

        const db = admin.firestore();

        try {
            // Get all solves from the old path
            const oldSolvesRef = db.collection("users").doc(userId).collection("timers");
            const oldSolvesSnapshot = await oldSolvesRef.get();

            if (oldSolvesSnapshot.empty) {
                return res.json({
                    message: "No solves to migrate",
                    count: 0
                });
            }

            // Count of migrated solves
            let migratedCount = 0;
            let errorCount = 0;

            // Process in batches of 500 (Firestore batch limit)
            const batches = [];
            let currentBatch = db.batch();
            let operationsInCurrentBatch = 0;

            // Process each solve
            for (const oldSolveDoc of oldSolvesSnapshot.docs) {
                try {
                    const oldSolveData = oldSolveDoc.data();
                    const oldSolveId = oldSolveDoc.id;

                    // Create a new document in the new path with the same ID
                    const newSolveRef = db.collection("users").doc(userId).collection("solves").doc(oldSolveId);

                    // Ensure timestamp is a proper Firestore timestamp
                    if (oldSolveData.timestamp && typeof oldSolveData.timestamp !== 'object') {
                        oldSolveData.timestamp = admin.firestore.Timestamp.fromDate(new Date(oldSolveData.timestamp));
                    }

                    // Add to batch
                    currentBatch.set(newSolveRef, oldSolveData);
                    operationsInCurrentBatch++;
                    migratedCount++;

                    // If batch is full, add it to the list and create a new one
                    if (operationsInCurrentBatch >= 500) {
                        batches.push(currentBatch);
                        currentBatch = db.batch();
                        operationsInCurrentBatch = 0;
                    }
                } catch (e) {
                    console.error(`Error processing solve ${oldSolveDoc.id}: ${e}`);
                    errorCount++;
                }
            }

            // Add the last batch if it has operations
            if (operationsInCurrentBatch > 0) {
                batches.push(currentBatch);
            }

            // Commit all batches
            for (const batch of batches) {
                await batch.commit();
            }

            return res.json({
                message: "Migration completed",
                migrated: migratedCount,
                errors: errorCount
            });
        } catch (e) {
            console.error(`Error during migration: ${e}`);
            return res.status(500).json({
                error: "Migration failed",
                message: e.message
            });
        }
    }
);

// HTTP function to get statistics for a user
exports.getStats = onRequest(
    {
        region: "us-central1",
        memory: "1GiB",
        cpu: 1,
        maxInstances: 10
    },
    async (req, res) => {
        const userId = req.query.userId;
        const cubeType = req.query.cubeType;
        const tagId = req.query.tagId;

        if (!userId || !cubeType || !tagId) {
            return res.status(400).json({
                error: "Missing required parameters"
            });
        }

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
                ao100: 0,
                recentSolves: []
            });
        }

        return res.json(statsDoc.data());
    }
);
