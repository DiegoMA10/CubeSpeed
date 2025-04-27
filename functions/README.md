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

- `getStatsAggregated`: Gets aggregated statistics for a user, cube type, and tag
- `recalculateStats`: Recalculates statistics for a user, cube type, and tag
- `calculateAverageOfN`: Calculates the average of N times, excluding the best and worst times

## Key Implementation Notes

- Standard deviation is calculated manually in JavaScript
- Firestore's count() method is used for counting documents
- Average calculation uses AggregateField.average with a fallback to manual calculation if aggregation fails
- All functions use the Firebase Functions 2nd Gen API
- Memory and CPU configurations have been optimized for each function:
  - `getStats`: 1GiB memory, 2 CPUs, 1 minimum instance for fast response
  - `updateStatsOnSolve` and `updateStatsOnDelete`: 512MiB memory, 1 CPU for efficient background processing
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
