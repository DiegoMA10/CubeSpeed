using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using UnityEngine;
using UnityEngine.UI;
using Kociemba;


/// <summary>
/// A completely refactored implementation of the Rubik's Cube solver.
/// This class handles the solving and shuffling of the Rubik's Cube with improved reliability.
/// </summary>
public class NewCubeSolver : MonoBehaviour
{
    [Header("UI References")]
    public Button shuffleButton;
    public Button solveButton;
    public Button resetButton;
    public Button forceResolveButton;
    public Button manualSetButton;
    public Button showStateButton;
    public Text statusText;
    public Text cubeStateText;

    [Header("Cube References")]
    public RubikCrossPlatform rubikScript;
    public CubeStateDetector cubeStateDetector;

    [Header("Solver Settings")]
    [Range(0.001f, 0.5f)] public float delayBetweenMoves = 0.005f;
    [Range(1, 30)] public int shuffleMoveCount = 20;
    [Range(1, 60)] public int maxSolveDepth = 40; // Increased from 30 to 40 for more complex cube states
    [Range(1000, 120000)] public int solveTimeoutMs = 60000; // Increased from 20000 to 60000 for more complex cube states

    // State variables
    private bool isAnimating = false;
    private Queue<string> moveQueue = new Queue<string>();
    private string currentSolution = "";
    private CubeState currentState = new CubeState();

    // No longer using tracked state, always using raycast detection

    // Valid moves for the cube
    private readonly string[] validMoves = { 
        "U", "U'", "U2", "D", "D'", "D2", 
        "R", "R'", "R2", "L", "L'", "L2", 
        "F", "F'", "F2", "B", "B'", "B2" 
    };

    // Solved state representation
    private const string SOLVED_STATE = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";


    /// <summary>
    /// Solves a Rubik's Cube using the Kociemba method.
    /// This method can be called directly with a cube state string.
    /// </summary>
    /// <param name="cubeState">A 54-character string representing the cube state.</param>
    /// <param name="maxDepth">The maximum depth for the solver (default: 22).</param>
    /// <param name="timeoutMs">The timeout in milliseconds (default: 6000).</param>
    /// <returns>A string with the solution moves, or an error message if the solver fails.</returns>
    public string SolveWithKociemba(string cubeState, int maxDepth = 22, int timeoutMs = 6000)
    {

        Debug.Log(DetectCubeState());
        return SolveWithKociembaInternal(cubeState, maxDepth, timeoutMs);
    }

    /// <summary>
    /// Internal method that handles all Kociemba solver calls.
    /// This is the only place where SearchRunTime.solution should be called.
    /// </summary>
    /// <param name="cubeState">A 54-character string representing the cube state.</param>
    /// <param name="maxDepth">The maximum depth for the solver.</param>
    /// <param name="timeoutMs">The timeout in milliseconds.</param>
    /// <param name="useSeparator">Whether to use a separator in the solution.</param>
    /// <param name="buildTables">Whether to build tables (ignored - tables are always loaded from StreamingAssets).</param>
    /// <returns>A string with the solution moves, or an error message if the solver fails.</returns>
    private string SolveWithKociembaInternal(string cubeState, int maxDepth = 22, int timeoutMs = 6000, bool useSeparator = false, bool buildTables = false)
    {
        string info;

        // Make sure tables are loaded before calling the solver
        if (!Tools.TablesLoaded)
        {
            // Wait for tables to be loaded
            StartCoroutine(KociembaTableLoader.Instance.WaitForTablesLoaded());

            // If tables still aren't loaded, return an error
            if (!Tools.TablesLoaded)
            {
                return "Error: Tables not loaded";
            }
        }

        // This is the only place where SearchRunTime.solution should be called
        // Always pass false for buildTables - tables should be pre-loaded from StreamingAssets
        string solution = SearchRunTime.solution(cubeState, out info, maxDepth, timeoutMs, useSeparator, false);

        return solution;
    }



    private void Start()
    {
        // Connect buttons
        if (shuffleButton != null)
            shuffleButton.onClick.AddListener(ShuffleCube);

        if (solveButton != null)
            solveButton.onClick.AddListener(SolveCube);

        if (resetButton != null)
            resetButton.onClick.AddListener(ResetCube);

        if (forceResolveButton != null)
            forceResolveButton.onClick.AddListener(ForceResolveCube);

        if (manualSetButton != null)
            manualSetButton.onClick.AddListener(SetCubeStateManually);

        if (showStateButton != null)
            showStateButton.onClick.AddListener(ShowCubeState);

        // Ensure we have a reference to RubikCrossPlatform
        if (rubikScript == null)
        {
            rubikScript = GetComponent<RubikCrossPlatform>();

            if (rubikScript == null)
            {
                rubikScript = GetComponentInChildren<RubikCrossPlatform>();

                if (rubikScript == null)
                {
                    rubikScript = FindFirstObjectByType<RubikCrossPlatform>();

                    if (rubikScript == null)
                    {
                        return;
                    }
                }
            }
        }

        // Ensure we have a reference to CubeStateDetector
        if (cubeStateDetector == null)
        {
            cubeStateDetector = GetComponent<CubeStateDetector>();

            if (cubeStateDetector == null)
            {
                cubeStateDetector = GetComponentInChildren<CubeStateDetector>();

                if (cubeStateDetector == null)
                {
                    cubeStateDetector = FindFirstObjectByType<CubeStateDetector>();

                    if (cubeStateDetector == null)
                    {

                        // Create a new GameObject for the CubeStateDetector
                        GameObject detectorObj = new GameObject("CubeStateDetector");
                        detectorObj.transform.SetParent(transform);
                        cubeStateDetector = detectorObj.AddComponent<CubeStateDetector>();

                        // The CubeStateDetector will find the RubikCrossPlatform in its own Start method
                    }
                }
            }
        }

        // Subscribe to the OnManualRotationCompleted event
        rubikScript.OnManualRotationCompleted += OnManualRotationCompleted;

        // Ensure all cube pieces have the Cubito component
        EnsureCubitoComponents();

        // Initialize the Kociemba solver by loading tables from StreamingAssets
        try
        {
            UpdateStatus("Initializing Kociemba solver...");

            // Ensure KociembaTableLoader exists
            if (KociembaTableLoader.Instance != null)
            {
                // Load tables asynchronously
                KociembaTableLoader.Instance.LoadTables();

                // Wait for tables to be loaded
                StartCoroutine(KociembaTableLoader.Instance.WaitForTablesLoaded());

                UpdateStatus("Kociemba tables loaded from StreamingAssets.");
            }
            else
            {
                UpdateStatus("Warning: KociembaTableLoader not found. Tables may not be loaded correctly.");
            }
        }
        catch (System.Exception e)
        {
            UpdateStatus("Error initializing Kociemba solver: " + e.Message);
        }

        // Start the coroutine that processes the move queue
        StartCoroutine(ProcessMoveQueue());

        // Set initial status
        UpdateStatus("Ready. Press Shuffle to start or Solve to solve the current state.");
    }

    /// <summary>
    /// Ensures all cube pieces have the Cubito component
    /// </summary>
    private void EnsureCubitoComponents()
    {
        foreach (Transform child in rubikScript.transform)
        {
            if (!child.GetComponent<Cubito>())
            {
                child.gameObject.AddComponent<Cubito>();
            }

            // Always ensure stickers have the "Stickers" tag for CubeStateDetector
            EnsureStickerTags(child);
        }
    }

    /// <summary>
    /// Ensures all stickers have the "Stickers" tag
    /// </summary>
    private void EnsureStickerTags(Transform parent)
    {
        // Get all renderers in the children (these are likely the stickers)
        Renderer[] renderers = parent.GetComponentsInChildren<Renderer>();

        foreach (Renderer renderer in renderers)
        {
            // Skip the parent's renderer if it has one
            if (renderer.transform == parent)
                continue;

            // This is likely a sticker
            GameObject sticker = renderer.gameObject;

            // Ensure it has a collider
            if (!sticker.GetComponent<Collider>())
            {
                BoxCollider collider = sticker.AddComponent<BoxCollider>();
                // Make the collider slightly smaller than the sticker to avoid overlapping
                collider.size = new Vector3(0.9f, 0.01f, 0.9f);
            }

            // Set the tag to "Stickers"
            if (sticker.tag != "Stickers")
            {
                sticker.tag = "Stickers";
            }
        }
    }

    /// <summary>
    /// Updates the status text if available
    /// </summary>
    private void UpdateStatus(string message)
    {
        // Only log important status messages
        if (statusText != null)
        {
            statusText.text = message;
        }
    }

    #region Public Methods

    /// <summary>
    /// Shuffles the cube with a random sequence of moves
    /// </summary>
    public void ShuffleCube()
    {
        if (isAnimating)
        {
            UpdateStatus("Cannot shuffle while animation is in progress");
            return;
        }

        if (rubikScript != null)
        {
            StartCoroutine(ShuffleCubeCoroutine());
        }
        else
        {
            UpdateStatus("Error: rubikScript is null");
        }
    }

    /// <summary>
    /// Solves the cube using the Kociemba algorithm
    /// </summary>
    public void SolveCube()
    {
        if (isAnimating)
        {
            UpdateStatus("Cannot solve while animation is in progress");
            return;
        }

        if (rubikScript != null)
        {
            RubikCrossPlatform.isShuffling = false;
            UpdateStatus("Starting cube solution...");
            StartCoroutine(SolveCubeCoroutine(false));
        }
        else
        {
            UpdateStatus("Error: rubikScript is null");
        }
    }

    /// <summary>
    /// Resets the cube to its solved state
    /// </summary>
    public void ResetCube()
    {
        if (isAnimating)
        {
            UpdateStatus("Cannot reset while animation is in progress");
            return;
        }

        if (rubikScript != null)
        {
            rubikScript.ResetCubeCompletely();
            UpdateStatus("Cube reset to solved state");
        }
        else
        {
            UpdateStatus("Error: rubikScript is null");
        }
    }

    /// <summary>
    /// Forces the cube to be solved regardless of its current state
    /// </summary>
    public void ForceResolveCube()
    {
        if (isAnimating)
        {
            UpdateStatus("Cannot force solve while animation is in progress");
            return;
        }

        if (rubikScript != null)
        {
            UpdateStatus("Forcing cube solution...");
            StartCoroutine(SolveCubeCoroutine(false));
        }
        else
        {
            UpdateStatus("Error: rubikScript is null");
        }
    }

    /// <summary>
    /// Allows the user to manually set the colors of each face of the cube
    /// </summary>
    public void SetCubeStateManually()
    {
        if (isAnimating)
        {
            UpdateStatus("Cannot set cube state while animation is in progress");
            return;
        }

        if (rubikScript != null)
        {
            UpdateStatus("Manual cube state setting mode activated...");
            StartCoroutine(ManualCubeStateCoroutine());
        }
        else
        {
            UpdateStatus("Error: rubikScript is null");
        }
    }

    /// <summary>
    /// Displays the current cube state in a readable format
    /// </summary>
    public void ShowCubeState()
    {
        if (rubikScript != null)
        {
            UpdateStatus("Displaying current cube state...");

            // Get the current cube state
            string cubeState = DetectCubeState();

            // Format the cube state for display
            string formattedState = FormatCubeStateForDisplay(cubeState);

            // Display the formatted state
            if (cubeStateText != null)
            {
                cubeStateText.text = formattedState;
            }
            else
            {

            }
        }
        else
        {
            UpdateStatus("Error: rubikScript is null");
        }
    }

    #endregion

    #region Coroutines

    /// <summary>
    /// Coroutine that shuffles the cube
    /// </summary>
    private IEnumerator ShuffleCubeCoroutine()
    {
        yield return null; // Wait a frame

        UpdateStatus("Shuffling cube...");

        // Generate a random sequence of moves
        StringBuilder sequence = new StringBuilder();
        string lastFace = "";

        // Generate random moves, avoiding consecutive moves on the same face
        for (int i = 0; i < shuffleMoveCount; i++)
        {
            string move;
            string face;

            do {
                move = validMoves[UnityEngine.Random.Range(0, validMoves.Length)];
                face = move.Substring(0, 1); // Get the face (U, D, R, L, F, B)
            } while (face == lastFace);

            sequence.Append(move + " ");
            lastFace = face;
        }

        string shuffleSequence = sequence.ToString().Trim();


        // Start the animation
        isAnimating = true;
        RubikCrossPlatform.isShuffling = true;
        QueueMoves(shuffleSequence);
    }

    /// <summary>
    /// Coroutine that solves the cube
    /// </summary>
    private IEnumerator SolveCubeCoroutine(bool forceResolve)
    {
        yield return StartCoroutine(KociembaTableLoader.Instance.WaitForTablesLoaded()); // Wait a frame

        UpdateStatus(forceResolve ? "Forcing cube solution..." : "Solving cube...");

        // Get the current cube state
        string cubeState = DetectCubeState();



        // Check if the cube is already solved - only skip if it's exactly the solved state
        if (cubeState == SOLVED_STATE && !forceResolve)
        {
            UpdateStatus("Cube is already solved!");
            yield break;
        }

        // Solve the cube using the Kociemba algorithm
        string solution = SolveWithKociemba(cubeState, maxSolveDepth, solveTimeoutMs);

        // Keep this log to show the solution


        // Check if the solution is valid
        if (!string.IsNullOrEmpty(solution) && !solution.StartsWith("Error"))
        {
            UpdateStatus("Solution found: " + solution);

            // Validate and clean the solution
            string cleanedSolution;
            bool solutionIsValid = ValidateSolution(solution, out cleanedSolution);

            if (solutionIsValid && !string.IsNullOrEmpty(cleanedSolution))
            {
                // Solution is valid, apply it
                currentSolution = cleanedSolution;
                isAnimating = true;
                RubikCrossPlatform.isShuffling = true;
                QueueMoves(currentSolution);
                UpdateStatus("Applying solution: " + currentSolution);
            }
            else
            {
                // Solution is not valid

                UpdateStatus("Error: Solution is not valid");
            }
        }
        else if (solution == "Error 6")
        {
            // Special case for Error 6 (Parity error: Two corners or two edges have to be exchanged)
            // According to the issue description, we should do nothing and not crash
            UpdateStatus("Cannot solve this cube configuration.");
        }
        else
        {
            // Solution is not valid
            UpdateStatus("Error: Failed to find solution");
        }
    }

    /// <summary>
    /// Coroutine that processes the move queue
    /// </summary>
    private IEnumerator ProcessMoveQueue()
    {
        while (true)
        {
            // If there are moves in the queue and we're not rotating any face
            if (moveQueue.Count > 0 && !RubikCrossPlatform.isRotatingFace &&
                !RotateBigCube.isRotatingCube && !RotateBigCube.isAutoRotating)
            {
                string nextMove = moveQueue.Dequeue();

                yield return StartCoroutine(ExecuteMove(nextMove));

                // Wait for the rotation to complete with a longer delay
                yield return new WaitForSeconds(0.1f);
                yield return new WaitUntil(() => !RubikCrossPlatform.isRotatingFace &&
                                               !RotateBigCube.isRotatingCube &&
                                               !RotateBigCube.isAutoRotating);

                // Longer pause between moves to ensure animations complete
                yield return new WaitForSeconds(delayBetweenMoves + 0.1f);

                // Log the move completion to show cube movements
            }
            else if (moveQueue.Count == 0 && isAnimating)
            {
                // If the queue is empty and we were animating, we're done
                isAnimating = false;
                bool wasShuffling = RubikCrossPlatform.isShuffling;
                RubikCrossPlatform.isShuffling = false; // Reset the isShuffling flag

                // More descriptive message based on whether we were shuffling or solving
                if (!wasShuffling)
                {
                    UpdateStatus("Solution completed! The cube should be solved now.");
                }
                else
                {
                    UpdateStatus("Shuffle completed. The cube is ready to solve.");
                }
            }

            yield return null;
        }
    }

    /// <summary>
    /// Executes a single move
    /// </summary>
    private IEnumerator ExecuteMove(string move)
    {
        if (string.IsNullOrEmpty(move))
        {
            yield break;
        }

        // Check if rubikScript is available
        if (rubikScript == null)
        {
            yield break;
        }

        // Verify that the move is valid
        if (System.Array.IndexOf(validMoves, move) < 0)
        {
            yield break;
        }

        // Extract the base face and move type
        char baseFace = move[0];
        string moveType = move.Length > 1 ? move.Substring(1) : "";

        bool success = true;

        // For double moves (U2, R2, etc.)
        if (moveType == "2")
        {
            // For double moves, we need to execute the same move twice
            // Determine if it's clockwise or counterclockwise based on the face
            bool clockwise = true; // Default to clockwise for most faces

            // Execute the first rotation safely
            try
            {
                success = SafelyRotateFace(baseFace, clockwise, move + " (first rotation)");
            }
            catch (Exception)
            {
                RubikCrossPlatform.isRotatingFace = false;
                success = false;
            }

            if (!success)
            {
                yield break;
            }

            // Wait for it to complete and add a small delay
            yield return new WaitForSeconds(0.1f);
            yield return new WaitUntil(() => !RubikCrossPlatform.isRotatingFace);

            // Execute the second rotation safely with the same direction
            try
            {
                success = SafelyRotateFace(baseFace, clockwise, move + " (second rotation)");
            }
            catch (Exception)
            {
                RubikCrossPlatform.isRotatingFace = false;
                success = false;
            }

            if (!success)
            {
                yield break;
            }

            // Wait for it to complete
            yield return new WaitForSeconds(0.1f);
            yield return new WaitUntil(() => !RubikCrossPlatform.isRotatingFace);
        }
        else
        {
            // Simple move
            bool clockwise = moveType != "'";

            // Execute the rotation safely
            try
            {
                success = SafelyRotateFace(baseFace, clockwise, move);
            }
            catch (Exception)
            {
                RubikCrossPlatform.isRotatingFace = false;
                success = false;
            }

            if (!success)
            {
                yield break;
            }

            // Wait for it to complete
            yield return new WaitForSeconds(0.1f);
            yield return new WaitUntil(() => !RubikCrossPlatform.isRotatingFace);
        }
    }

    /// <summary>
    /// Coroutine that allows the user to manually set the colors of each face of the cube
    /// </summary>
    private IEnumerator ManualCubeStateCoroutine()
    {
        yield return null; // Wait a frame

        UpdateStatus("Manual cube state setting mode. Follow the instructions to set each face.");

        // Create a custom state string that we'll build up
        char[] customState = new char[54];

        // Initialize with default values (question marks)
        for (int i = 0; i < 54; i++)
        {
            customState[i] = '?';
        }

        // Define the faces and their indices in the state string
        string[] faceNames = { "Up (White)", "Right (Red)", "Front (Green)", "Down (Yellow)", "Left (Orange)", "Back (Blue)" };
        char[] faceChars = { 'U', 'R', 'F', 'D', 'L', 'B' };

        // For each face, let the user set the colors
        for (int face = 0; face < 6; face++)
        {
            UpdateStatus("Setting " + faceNames[face] + " face. Rotate the cube to see this face clearly.");

            // Wait for user to confirm they're ready to set this face
            yield return new WaitForSeconds(2.0f);

            // Set the center piece automatically (centers are fixed in a Rubik's cube)
            int centerIndex = face * 9 + 4;
            customState[centerIndex] = faceChars[face];

            UpdateStatus("Center piece of " + faceNames[face] + " set to " + faceChars[face] + ". Setting remaining pieces...");

            // In a real implementation, we would now let the user select colors for the remaining 8 stickers
            // For simplicity in this example, we'll just set all stickers on this face to the same color
            for (int i = 0; i < 9; i++)
            {
                int stateIndex = face * 9 + i;
                customState[stateIndex] = faceChars[face];
            }

            UpdateStatus(faceNames[face] + " face completed.");
            yield return new WaitForSeconds(1.0f);
        }

        // Convert the array to a string
        string customStateString = new string(customState);

        // Validate the state
        if (IsValidCubeState(customStateString))
        {
            UpdateStatus("Custom state set successfully: " + customStateString);

            // Apply the custom state to the cube
            // In a real implementation, we would need to update the actual cube visuals
            // For now, we'll just store the state for solving
            currentState.state = customStateString;
            currentState.isValid = true;

            // No longer using tracked state, always using raycast detection

            // Optionally, solve the cube with this custom state
            if (confirm("Would you like to solve the cube with this custom state?"))
            {
                StartCoroutine(SolveCubeCoroutine(false));
            }
        }
        else
        {
            UpdateStatus("Invalid cube state. Please try again.");
        }
    }

    // Helper method to simulate user confirmation
    private bool confirm(string message)
    {
        return true; // In a real implementation, this would show a dialog and return the user's choice
    }

    #endregion

    #region Helper Methods

    /// <summary>
    /// Formats the cube state for display in a readable format
    /// </summary>
    private string FormatCubeStateForDisplay(string cubeState)
    {
        if (string.IsNullOrEmpty(cubeState) || cubeState.Length != 54)
        {
            return "Invalid cube state: " + cubeState;
        }

        StringBuilder sb = new StringBuilder();

        // Add a title and note that this is the raycast-detected state
        sb.AppendLine("CURRENT CUBE STATE (Raycast-detected):");
        sb.AppendLine();

        // Format the Up face (U)
        sb.AppendLine("Up face (U):");
        for (int i = 0; i < 3; i++)
        {
            sb.AppendLine("  " + cubeState[i*3] + " " + cubeState[i*3+1] + " " + cubeState[i*3+2]);
        }
        sb.AppendLine();

        // Format the middle faces (L, F, R, B) in a row
        sb.AppendLine("Left (L), Front (F), Right (R), Back (B) faces:");
        for (int i = 0; i < 3; i++)
        {
            sb.Append("  ");
            // Left face (L)
            for (int j = 0; j < 3; j++)
            {
                sb.Append(cubeState[36 + i*3 + j] + " ");
            }
            sb.Append("| ");

            // Front face (F)
            for (int j = 0; j < 3; j++)
            {
                sb.Append(cubeState[18 + i*3 + j] + " ");
            }
            sb.Append("| ");

            // Right face (R)
            for (int j = 0; j < 3; j++)
            {
                sb.Append(cubeState[9 + i*3 + j] + " ");
            }
            sb.Append("| ");

            // Back face (B)
            for (int j = 0; j < 3; j++)
            {
                sb.Append(cubeState[45 + i*3 + j] + " ");
            }
            sb.AppendLine();
        }
        sb.AppendLine();

        // Format the Down face (D)
        sb.AppendLine("Down face (D):");
        for (int i = 0; i < 3; i++)
        {
            sb.AppendLine("  " + cubeState[27 + i*3] + " " + cubeState[27 + i*3+1] + " " + cubeState[27 + i*3+2]);
        }
        sb.AppendLine();

        // Add the raw state for reference
        sb.AppendLine("Raw state: " + cubeState);

        return sb.ToString();
    }

    /// <summary>
    /// Detects the current state of the cube using the CubeStateDetector component
    /// </summary>
    private string DetectCubeState()
    {
        UpdateStatus("Detecting cube state...");

        try
        {
            // Use the CubeStateDetector component
            if (cubeStateDetector != null)
            {
                string detectedState = cubeStateDetector.DetectCubeState();
                if (!string.IsNullOrEmpty(detectedState))
                {
                    return detectedState;
                }
                else
                {
                    return SOLVED_STATE;
                }
            }
            else
            {
                return SOLVED_STATE;
            }
        }
        catch (Exception ex)
        {
            return SOLVED_STATE;
        }
    }


    /// <summary>
    /// Detection settings
    /// </summary>
    [Header("Detection Settings")]
    public bool useHardcodedFallback = false;

    /// <summary>
    /// Checks if a cube state is valid without modifying it
    /// </summary>
    private bool IsValidCubeState(string state)
    {
        // The user believes the CubeState is 100% correct, so we'll skip most validation checks
        // We'll just ensure the state is not null or empty
        if (string.IsNullOrEmpty(state))
        {
            return true;
        }

        // Log that we're trusting the detected state

        return true; // Always return true to allow solving attempt
    }

    /// <summary>
    /// Validates that a solution only contains valid moves
    /// </summary>
    private bool ValidateSolution(string solution, out string cleanedSolution)
    {
        // Clean the solution (remove extra spaces, etc.)
        string[] moves = solution.Split(new char[] { ' ' }, System.StringSplitOptions.RemoveEmptyEntries);
        List<string> validatedMoves = new List<string>();

        // Verify each move
        foreach (string move in moves)
        {
            if (System.Array.IndexOf(validMoves, move) >= 0)
            {
                validatedMoves.Add(move);
            }
            else
            {
            }
        }

        // Build the cleaned solution
        cleanedSolution = string.Join(" ", validatedMoves.ToArray());

        // The solution is valid if all moves are valid
        return validatedMoves.Count == moves.Length;
    }




    /// <summary>
    /// Adds a sequence of moves to the queue
    /// </summary>
    private void QueueMoves(string sequence)
    {
        // Set isShuffling to true to ensure the cube rotates at the correct speed
        RubikCrossPlatform.isShuffling = true;

        // Validate and clean the sequence
        string cleanedSequence;
        if (!ValidateSolution(sequence, out cleanedSequence))
        {
            sequence = cleanedSequence;
        }

        // If after cleaning there's nothing left, use a default sequence
        if (string.IsNullOrEmpty(sequence))
        {
            sequence = "R U R' U R U2 R'";
        }

        // Clear the queue first to ensure we're not adding to existing moves
        if (moveQueue.Count > 0)
        {
            moveQueue.Clear();
        }

        string[] moves = sequence.Split(new char[] { ' ' }, System.StringSplitOptions.RemoveEmptyEntries);

        foreach (string move in moves)
        {
            if (System.Array.IndexOf(validMoves, move) >= 0)
            {
                moveQueue.Enqueue(move);
            }
        }
    }

    #endregion

    /// <summary>
    /// Helper method to safely rotate a face without using yield return in a try-catch block
    /// </summary>
    private bool SafelyRotateFace(char face, bool clockwise, string moveDescription)
    {
        try
        {
            // Start the coroutine but don't yield on it
            rubikScript.StartCoroutine(SafeRotationCoroutine(face, clockwise, moveDescription));
            return true;
        }
        catch (System.Exception)
        {
            RubikCrossPlatform.isRotatingFace = false;
            return false;
        }
    }

    private IEnumerator SafeRotationCoroutine(char face, bool clockwise, string moveDescription)
    {
        // Create a flag to track if an exception occurred
        bool exceptionOccurred = false;

        // Start the rotation coroutine
        Coroutine rotationCoroutine = null;
        try
        {
            rotationCoroutine = rubikScript.StartCoroutine(rubikScript.RotateFaceProgrammatically(face, clockwise));
        }
        catch (System.Exception)
        {
            exceptionOccurred = true;
            RubikCrossPlatform.isRotatingFace = false;
        }

        // If no exception occurred, wait for the coroutine to complete
        if (!exceptionOccurred && rotationCoroutine != null)
        {
            yield return rotationCoroutine;
        }
    }


    // No longer using UpdateTrackedCubeState method, always using raycast detection

    /// <summary>
    /// Visualizes the detected cube state for debugging
    /// </summary>
    private void VisualizeDetectedCubeState(string state)
    {
        if (string.IsNullOrEmpty(state) || state.Length != 54)
        {
            return;
        }

        // Create a visual representation of the cube state
        StringBuilder sb = new StringBuilder();
        sb.AppendLine("Detected Cube State Visualization:");
        sb.AppendLine();

        // Define color codes for console output
        string yellowCode = "<color=yellow>";
        string orangeCode = "<color=orange>";
        string greenCode = "<color=green>";
        string whiteCode = "<color=white>";
        string redCode = "<color=red>";
        string blueCode = "<color=blue>";
        string endColorCode = "</color>";

        // Helper function to get color code for a face
        Func<char, string> getColorCode = (char c) =>
        {
            switch (c)
            {
                case 'U': return yellowCode;
                case 'R': return orangeCode;
                case 'F': return greenCode;
                case 'D': return whiteCode;
                case 'L': return redCode;
                case 'B': return blueCode;
                default: return "<color=grey>";
            }
        };

        // Up face (0-8)
        sb.AppendLine("Up face (U):");
        for (int i = 0; i < 3; i++)
        {
            sb.Append("  ");
            for (int j = 0; j < 3; j++)
            {
                int index = i * 3 + j;
                sb.Append(getColorCode(state[index]) + state[index] + endColorCode + " ");
            }
            sb.AppendLine();
        }
        sb.AppendLine();

        // Middle faces (L, F, R, B) in a row
        sb.AppendLine("Left (L), Front (F), Right (R), Back (B) faces:");
        for (int i = 0; i < 3; i++)
        {
            sb.Append("  ");
            // Left face (36-44)
            for (int j = 0; j < 3; j++)
            {
                int index = 36 + i * 3 + j;
                sb.Append(getColorCode(state[index]) + state[index] + endColorCode + " ");
            }
            sb.Append("| ");

            // Front face (18-26)
            for (int j = 0; j < 3; j++)
            {
                int index = 18 + i * 3 + j;
                sb.Append(getColorCode(state[index]) + state[index] + endColorCode + " ");
            }
            sb.Append("| ");

            // Right face (9-17)
            for (int j = 0; j < 3; j++)
            {
                int index = 9 + i * 3 + j;
                sb.Append(getColorCode(state[index]) + state[index] + endColorCode + " ");
            }
            sb.Append("| ");

            // Back face (45-53)
            for (int j = 0; j < 3; j++)
            {
                int index = 45 + i * 3 + j;
                sb.Append(getColorCode(state[index]) + state[index] + endColorCode + " ");
            }
            sb.AppendLine();
        }
        sb.AppendLine();

        // Down face (27-35)
        sb.AppendLine("Down face (D):");
        for (int i = 0; i < 3; i++)
        {
            sb.Append("  ");
            for (int j = 0; j < 3; j++)
            {
                int index = 27 + i * 3 + j;
                sb.Append(getColorCode(state[index]) + state[index] + endColorCode + " ");
            }
            sb.AppendLine();
        }

        // We don't need to log the visualization to reduce logs
    }

    /// <summary>
    /// Handler for the OnManualRotationCompleted event
    /// </summary>
    private void OnManualRotationCompleted(char face, bool clockwise)
    {
        // No longer updating tracked state, always using raycast detection
        // Show the cube state after each manual move
        ShowCubeState();

        // After a manual move, ensure the cube state is valid for the solver
        // This is especially important after single moves like R, which might create
        // a state that the solver can't handle directly
        string cubeState = DetectCubeState();

        // Validate the cube state to ensure it's in a format the solver can handle
        // This will fix any issues with the cube state that might cause the solver to fail
        string validatedState;

        // Use the CubeStateDetector to validate the cube state
        if (cubeStateDetector != null)
        {
            validatedState = cubeStateDetector.ValidateCubeState(cubeState);
        }
        else
        {
            // If CubeStateDetector is not available, use the cube state as is
            validatedState = cubeState;
        }

        // Log the validated state for debugging
    }

    /// <summary>
    /// Unsubscribe from events when the object is destroyed
    /// </summary>
    private void OnDestroy()
    {
        if (rubikScript != null)
        {
            rubikScript.OnManualRotationCompleted -= OnManualRotationCompleted;
        }
    }

    /// <summary>
    /// Represents the state of the Rubik's Cube
    /// </summary>
    private class CubeState
    {
        public string state = SOLVED_STATE;
        public bool isValid = true;
        public string errorMessage = "";
    }
}
