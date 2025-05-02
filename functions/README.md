# Firebase Functions for CubeSpeed

This directory contains the Firebase Cloud Functions for the CubeSpeed application. These functions handle statistics calculations for cube solving times.

## Migration from Python to Node.js

These functions were migrated from Python to Node.js (JavaScript) while maintaining the exact same functionality:

1. `updateStatsOnSolve`: Firestore trigger that runs when a solve is added or updated
2. `updateStatsOnDelete`: Firestore trigger that runs when a solve is deleted
3. `getStats`: HTTP function to get statistics for a user

## Migration to Node.js 22

The functions have been updated to use Node.js 22, which is the latest LTS version of Node.js. This update provides:

1. Improved performance and security
2. Access to the latest JavaScript features
3. Better compatibility with modern dependencies
4. Longer support timeline

## Migration to 2nd Gen Firebase Functions

The functions have been migrated to use Firebase Functions 2nd Generation:

1. Updated imports to use the modular v2 API:
   - `onDocumentWritten` and `onDocumentDeleted` from `firebase-functions/v2/firestore`
   - `onRequest` from `firebase-functions/v2/https`

2. Updated function signatures to use the new v2 patterns

3. Updated firebase.json to include 2nd Gen configuration:
   - Added `"runtime": "nodejs22"`
   - Added `"region": "us-central1"`
   - Added `"v2": true`

## Helper Functions

- `calculateAverageOfN`: Calculates the average of N times, excluding the best and worst times

## Performance Optimization

The functions have been optimized to significantly reduce Firestore reads:

1. **Incremental Statistics Calculation**: Instead of reading all solves each time, statistics are updated incrementally.

2. **recentSolves Array**: The stats document stores an optimized array of the last 100 solves, which allows:
   - Calculating Ao5, Ao12, Ao50, and Ao100 without additional Firestore reads
   - Maintaining a sliding window of recent solves
   - Reducing reads from ~100 per solve to just 1

3. **Efficient Updates**: Both `updateStatsOnSolve` and `updateStatsOnDelete` efficiently maintain the recentSolves array:
   - For new solves: Add to the beginning of the array and remove oldest if over 100
   - For updates: Replace the existing solve in the array
   - For deletions: Remove the solve from the array
   - All operations only require reading and writing the stats document once

4. **Storage Optimization**: The recentSolves array only stores essential fields for each solve:
   - ID, duration, status, timestamp, cube type, and tag ID
   - This minimizes the document size while maintaining all necessary information

5. **Response Size Optimization**: The `getStats` function returns only the calculated statistics to reduce data transfer.

## Key Implementation Notes

- Standard deviation is calculated manually in JavaScript
- All statistics (count, sum, best, average, Ao5, etc.) are calculated incrementally
- All functions use the Firebase Functions 2nd Gen API
- Memory and CPU configurations have been optimized for each function:
  - `getStats`: 1GiB memory, 2 CPUs for fast response
  - `updateStatsOnSolve` and `updateStatsOnDelete`: 1GiB memory, 2 CPUs for efficient background processing
  - See [RESOURCE_CONFIG.md](./RESOURCE_CONFIG.md) for detailed configuration information

## Deployment Instructions

1. Make sure you have the Firebase CLI installed:
   ```
   npm install -g firebase-tools
   ```

2. Login to Firebase:
   ```
   firebase login
   ```

3. Navigate to the project root directory and deploy the functions:
   ```
   firebase deploy --only functions
   ```

## Local Testing

To test the functions locally:

1. Navigate to the functions directory:
   ```
   cd functions
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Start the Firebase emulator:
   ```
   npm run serve
   ```

This will start the Firebase Functions emulator, allowing you to test the functions locally before deployment.
