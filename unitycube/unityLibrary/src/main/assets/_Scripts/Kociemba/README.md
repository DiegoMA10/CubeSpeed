# Kociemba Rubik's Cube Solver for Unity (Android Compatible)

This is a modified version of the Kociemba Rubik's Cube solver that works on Android devices. The original solver uses standard file I/O operations which don't work properly on Android, so this version uses UnityWebRequest to load the tables from the StreamingAssets folder.

## Setup Instructions

### 1. Generate the Tables

First, you need to generate the tables that the solver uses. This should be done in the Unity Editor (not on Android):

1. Create a new GameObject in your scene
2. Attach the `KociembaTableLoader` script to it
3. Attach the `KociembaExample` script to it (optional, for testing)
4. Run the game in the Editor
5. The tables will be generated and saved to `Assets/Kociemba/Tables/`

### 2. Copy Tables to StreamingAssets

After generating the tables, you need to copy them to the 1 folder so they can be accessed on Android:

1. Create a folder structure in your StreamingAssets folder: `Assets/StreamingAssets/Kociemba/Tables/`
2. Copy all files from `Assets/Kociemba/Tables/` to `Assets/StreamingAssets/Kociemba/Tables/`
3. Make sure these files are included in your build (they should be automatically if they're in StreamingAssets)

### 3. Use the Solver in Your Game

There are two ways to use the solver:

#### Option 1: Using KociembaTableLoader (Recommended)

```csharp
// Get or create the table loader
KociembaTableLoader tableLoader = KociembaTableLoader.Instance;

// Wait for tables to be loaded before solving
StartCoroutine(WaitAndSolve());

// Coroutine to wait for tables
private IEnumerator WaitAndSolve()
{
    // Wait for tables to be loaded
    yield return StartCoroutine(tableLoader.WaitForTablesLoaded());
    
    // Now solve the cube
    string cubeState = "DUUBULDBFRBFRRULLLBRDFFFBLURDBFDFDRFRULBLUFDURRBLBDUDL"; // Example scrambled cube
    string info;
    string solution = Search.solution(cubeState, out info);
    
    Debug.Log("Solution: " + solution);
}
```

#### Option 2: Check Tables Loaded Status Manually

```csharp
// Check if tables are loaded
if (Tools.TablesLoaded)
{
    // Solve the cube
    string cubeState = "DUUBULDBFRBFRRULLLBRDFFFBLURDBFDFDRFRULBLUFDURRBLBDUDL"; // Example scrambled cube
    string info;
    string solution = Search.solution(cubeState, out info);
    
    Debug.Log("Solution: " + solution);
}
else
{
    Debug.LogWarning("Tables are not loaded yet!");
}
```

## Cube State Format

The cube state is represented as a string of 54 characters, where each character represents the color of a facelet:

```
             |************|
             |*U1**U2**U3*|
             |************|
             |*U4**U5**U6*|
             |************|
             |*U7**U8**U9*|
             |************|
 ************|************|************|************
 *L1**L2**L3*|*F1**F2**F3*|*R1**R2**R3*|*B1**B2**B3*
 ************|************|************|************
 *L4**L5**L6*|*F4**F5**F6*|*R4**R5**R6*|*B4**B5**B6*
 ************|************|************|************
 *L7**L8**L9*|*F7**F8**F9*|*R7**R8**R9*|*B7**B8**B9*
 ************|************|************|************
             |************|
             |*D1**D2**D3*|
             |************|
             |*D4**D5**D6*|
             |************|
             |*D7**D8**D9*|
             |************|
```

The order of the facelets in the cube state string is:
```
U1, U2, U3, U4, U5, U6, U7, U8, U9, 
R1, R2, R3, R4, R5, R6, R7, R8, R9, 
F1, F2, F3, F4, F5, F6, F7, F8, F9, 
D1, D2, D3, D4, D5, D6, D7, D8, D9, 
L1, L2, L3, L4, L5, L6, L7, L8, L9, 
B1, B2, B3, B4, B5, B6, B7, B8, B9
```

Where:
- U = Up (usually white)
- R = Right (usually red)
- F = Front (usually green)
- D = Down (usually yellow)
- L = Left (usually orange)
- B = Back (usually blue)

## Troubleshooting

If you encounter issues with the solver on Android:

1. Make sure the tables are correctly copied to the StreamingAssets folder
2. Check the logs for any error messages related to loading the tables
3. Ensure you're waiting for the tables to be loaded before trying to solve a cube
4. If you're getting "Error 1" from the solver, check that your cube state string is correctly formatted

## Example

See the `KociembaExample.cs` script for a complete example of how to use the solver.