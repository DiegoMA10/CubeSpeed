# Firebase Functions Resource Configuration

This document provides detailed information about the memory and CPU configurations for the Firebase Functions in the CubeSpeed application.

## Resource Configuration Overview

Each Firebase Function has been configured with specific memory and CPU allocations to optimize performance and cost:

| Function | Memory | CPU | Min Instances | Max Instances | Purpose |
|----------|--------|-----|---------------|---------------|---------|
| `getStats` | 1GiB | 2 | 1 | 10 | HTTP endpoint for retrieving statistics |
| `updateStatsOnSolve` | 512MiB | 1 | 0 | 10 | Background trigger for solve updates |
| `updateStatsOnDelete` | 512MiB | 1 | 0 | 10 | Background trigger for solve deletions |

## Configuration Details

### getStats

```javascript
exports.getStats = onRequest(
  {
    region: "us-central1",
    memory: "1GiB",       // Higher memory for handling larger datasets
    cpu: 2,               // 2 CPUs for faster processing
    minInstances: 1,      // Always active to avoid cold starts
    maxInstances: 10      // Limit concurrent executions
  },
  async (req, res) => {
    // Function implementation
  }
);
```

- **Higher Memory (1GiB)**: Allocated to handle potentially large datasets when retrieving statistics
- **2 CPUs**: Provides faster processing for complex calculations
- **Minimum Instances (1)**: Keeps at least one instance warm to avoid cold starts, improving response times
- **Maximum Instances (10)**: Limits concurrent executions to control costs

### updateStatsOnSolve and updateStatsOnDelete

```javascript
exports.updateStatsOnSolve = onDocumentWritten(
  { 
    document: 'users/{userId}/solves/{solveId}',
    region: "us-central1",
    memory: "512MiB",     // Sufficient for background processing
    cpu: 1,               // Single CPU is adequate
    maxInstances: 10      // Limit concurrent executions
  },
  async (event) => {
    // Function implementation
  }
);
```

- **Moderate Memory (512MiB)**: Sufficient for background processing tasks
- **1 CPU**: Adequate for sequential operations
- **No Minimum Instances**: Cold starts are acceptable for background operations
- **Maximum Instances (10)**: Prevents excessive concurrent executions

## Cost Optimization

These configurations balance performance and cost:

- The `getStats` function has higher resources to ensure responsive user experience
- Background functions use more modest resources to minimize costs
- Maximum instances are capped to prevent unexpected billing spikes

## Monitoring Recommendations

To ensure these configurations are optimal:

1. Monitor function execution times in the Firebase Console
2. Check for memory-related errors in the logs
3. Adjust configurations if performance issues are observed
4. Consider increasing resources during peak usage periods
