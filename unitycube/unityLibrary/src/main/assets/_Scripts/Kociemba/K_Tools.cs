using System;
using System.IO;
using System.Collections;
using System.Runtime.Serialization.Formatters.Binary;
using UnityEngine;
using UnityEngine.Networking;
using Random = System.Random;

namespace Kociemba
{
    public class Tools
    {
        // Flag to track if tables are loaded
        private static bool tablesLoaded = false;

        // Property to check if tables are loaded
        public static bool TablesLoaded => tablesLoaded;
        // ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        // Check if the cube string s represents a solvable cube.
        // 0: Cube is solvable
        // -1: There is not exactly one facelet of each colour
        // -2: Not all 12 edges exist exactly once
        // -3: Flip error: One edge has to be flipped
        // -4: Not all corners exist exactly once
        // -5: Twist error: One corner has to be twisted
        // -6: Parity error: Two corners or two edges have to be exchanged
        // 
        /// <summary>
        /// Check if the cube definition string s represents a solvable cube.
        /// </summary>
        /// <param name="s"> is the cube definition string , see <seealso cref="Facelet"/> </param>
        /// <returns> 0: Cube is solvable<br>
        ///         -1: There is not exactly one facelet of each colour<br>
        ///         -2: Not all 12 edges exist exactly once<br>
        ///         -3: Flip error: One edge has to be flipped<br>
        ///         -4: Not all 8 corners exist exactly once<br>
        ///         -5: Twist error: One corner has to be twisted<br>
        ///         -6: Parity error: Two corners or two edges have to be exchanged </returns>
        public static int verify(string s)
        {
            int[] count = new int[6];
            try
            {
                for (int i = 0; i < 54; i++)
                {
                    count[(int)CubeColor.Parse(typeof(CubeColor), i.ToString())]++;
                }
            }
            catch (Exception)
            {
                return -1;
            }

            for (int i = 0; i < 6; i++)
            {
                if (count[i] != 9)
                {
                    return -1;
                }
            }

            FaceCube fc = new FaceCube(s);
            CubieCube cc = fc.toCubieCube();

            return cc.verify();
        }

        /// <summary>
        /// Generates a random cube. </summary>
        /// <returns> A random cube in the string representation. Each cube of the cube space has the same probability. </returns>
        public static string randomCube()
        {
            CubieCube cc = new CubieCube();
            Random gen = new Random();
            cc.setFlip((short)gen.Next(CoordCube.N_FLIP));
            cc.setTwist((short)gen.Next(CoordCube.N_TWIST));
            do
            {
                cc.setURFtoDLB(gen.Next(CoordCube.N_URFtoDLB));
                cc.setURtoBR(gen.Next(CoordCube.N_URtoBR));
            } while ((cc.edgeParity() ^ cc.cornerParity()) != 0);
            FaceCube fc = cc.toFaceCube();
            return fc.to_fc_String();
        }


        // https://stackoverflow.com/questions/7742519/c-sharp-export-write-multidimension-array-to-file-csv-or-whatever
        // Kristian Fenn: https://stackoverflow.com/users/989539/kristian-fenn

        // Disabled to prevent writing tables to disk - tables should be pre-generated and placed in StreamingAssets
        public static void SerializeTable(string filename, short[,] array)
        {
            Debug.LogWarning("SerializeTable is disabled. Tables should be pre-generated and placed in StreamingAssets/Kociemba/Tables.");
            // Do nothing - tables should be pre-generated and placed in StreamingAssets
        }

        public static short[,] DeserializeTable(string filename)
        {
            // For Android, we need to use a different approach
            if (Application.platform == RuntimePlatform.Android)
            {
                Debug.LogWarning("Using Android-specific table loading for: " + filename);
                // Return a placeholder array - this will be replaced by the async loading
                return new short[1, 1];
            }
            else
            {
                try
                {
                    // Use StreamingAssets path for all platforms
                    string path = Path.Combine(Application.streamingAssetsPath, "Kociemba", "Tables", filename);
                    if (File.Exists(path))
                    {
                        Stream s = File.Open(path, FileMode.Open);
                        BinaryFormatter bf = new BinaryFormatter();
                        short[,] array = (short[,])bf.Deserialize(s);
                        s.Close();
                        return array;
                    }
                    else
                    {
                        Debug.LogError("Table file not found: " + path);
                        return new short[1, 1]; // Return a placeholder array
                    }
                }
                catch (Exception e)
                {
                    Debug.LogError("Error loading table " + filename + ": " + e.Message);
                    return new short[1, 1]; // Return a placeholder array
                }
            }
        }

        // Coroutine to load all tables asynchronously for Android
        public static IEnumerator LoadTablesAsync(MonoBehaviour caller, System.Action onComplete = null)
        {
            if (Application.platform == RuntimePlatform.Android)
            {
                Debug.Log("Starting to load Kociemba tables for Android...");

                // Load movement tables
                yield return caller.StartCoroutine(LoadTableAsync("twist", (result) => CoordCubeTables.twist = result));
                yield return caller.StartCoroutine(LoadTableAsync("flip", (result) => CoordCubeTables.flip = result));
                yield return caller.StartCoroutine(LoadTableAsync("FRtoBR", (result) => CoordCubeTables.FRtoBR = result));
                yield return caller.StartCoroutine(LoadTableAsync("URFtoDLF", (result) => CoordCubeTables.URFtoDLF = result));
                yield return caller.StartCoroutine(LoadTableAsync("URtoDF", (result) => CoordCubeTables.URtoDF = result));
                yield return caller.StartCoroutine(LoadTableAsync("URtoUL", (result) => CoordCubeTables.URtoUL = result));
                yield return caller.StartCoroutine(LoadTableAsync("UBtoDF", (result) => CoordCubeTables.UBtoDF = result));
                yield return caller.StartCoroutine(LoadTableAsync("MergeURtoULandUBtoDF", (result) => CoordCubeTables.MergeURtoULandUBtoDF = result));

                // Load pruning tables
                yield return caller.StartCoroutine(LoadSbyteArrayAsync("Slice_URFtoDLF_Parity_Prun", (result) => CoordCubeTables.Slice_URFtoDLF_Parity_Prun = result));
                yield return caller.StartCoroutine(LoadSbyteArrayAsync("Slice_URtoDF_Parity_Prun", (result) => CoordCubeTables.Slice_URtoDF_Parity_Prun = result));
                yield return caller.StartCoroutine(LoadSbyteArrayAsync("Slice_Twist_Prun", (result) => CoordCubeTables.Slice_Twist_Prun = result));
                yield return caller.StartCoroutine(LoadSbyteArrayAsync("Slice_Flip_Prun", (result) => CoordCubeTables.Slice_Flip_Prun = result));

                tablesLoaded = true;
                Debug.Log("All Kociemba tables loaded successfully for Android!");
            }
            else
            {
                // For non-Android platforms, tables are loaded synchronously
                tablesLoaded = true;
            }

            // Call the completion callback if provided
            onComplete?.Invoke();
        }

        // Coroutine to load a single table asynchronously
        private static IEnumerator LoadTableAsync(string filename, System.Action<short[,]> onComplete)
        {
            string path = Path.Combine(Application.streamingAssetsPath, "Kociemba", "Tables", filename);

            using (UnityWebRequest www = UnityWebRequest.Get(path))
            {
                yield return www.SendWebRequest();

                if (www.result != UnityWebRequest.Result.Success)
                {
                    Debug.LogError($"Failed to load table {filename}: {www.error}");
                    onComplete?.Invoke(new short[1, 1]); // Return a placeholder array
                }
                else
                {
                    try
                    {
                        MemoryStream memStream = new MemoryStream(www.downloadHandler.data);
                        BinaryFormatter bf = new BinaryFormatter();
                        short[,] array = (short[,])bf.Deserialize(memStream);
                        memStream.Close();

                        Debug.Log($"Successfully loaded table {filename}");
                        onComplete?.Invoke(array);
                    }
                    catch (Exception e)
                    {
                        Debug.LogError($"Error deserializing table {filename}: {e.Message}");
                        onComplete?.Invoke(new short[1, 1]); // Return a placeholder array
                    }
                }
            }
        }

        // Disabled to prevent writing tables to disk - tables should be pre-generated and placed in StreamingAssets
        public static void SerializeSbyteArray(string filename, sbyte[] array)
        {
            Debug.LogWarning("SerializeSbyteArray is disabled. Tables should be pre-generated and placed in StreamingAssets/Kociemba/Tables.");
            // Do nothing - tables should be pre-generated and placed in StreamingAssets
        }

        public static sbyte[] DeserializeSbyteArray(string filename)
        {
            // For Android, we need to use a different approach
            if (Application.platform == RuntimePlatform.Android)
            {
                Debug.LogWarning("Using Android-specific sbyte array loading for: " + filename);
                // Return a placeholder array - this will be replaced by the async loading
                return new sbyte[1];
            }
            else
            {
                try
                {
                    // Use StreamingAssets path for all platforms
                    string path = Path.Combine(Application.streamingAssetsPath, "Kociemba", "Tables", filename);
                    if (File.Exists(path))
                    {
                        Stream s = File.Open(path, FileMode.Open);
                        BinaryFormatter bf = new BinaryFormatter();
                        sbyte[] array = (sbyte[])bf.Deserialize(s);
                        s.Close();
                        return array;
                    }
                    else
                    {
                        Debug.LogError("Table file not found: " + path);
                        return new sbyte[1]; // Return a placeholder array
                    }
                }
                catch (Exception e)
                {
                    Debug.LogError("Error loading sbyte array " + filename + ": " + e.Message);
                    return new sbyte[1]; // Return a placeholder array
                }
            }
        }

        // Coroutine to load a single sbyte array asynchronously
        private static IEnumerator LoadSbyteArrayAsync(string filename, System.Action<sbyte[]> onComplete)
        {
            string path = Path.Combine(Application.streamingAssetsPath, "Kociemba", "Tables", filename);

            using (UnityWebRequest www = UnityWebRequest.Get(path))
            {
                yield return www.SendWebRequest();

                if (www.result != UnityWebRequest.Result.Success)
                {
                    Debug.LogError($"Failed to load sbyte array {filename}: {www.error}");
                    onComplete?.Invoke(new sbyte[1]); // Return a placeholder array
                }
                else
                {
                    try
                    {
                        MemoryStream memStream = new MemoryStream(www.downloadHandler.data);
                        BinaryFormatter bf = new BinaryFormatter();
                        sbyte[] array = (sbyte[])bf.Deserialize(memStream);
                        memStream.Close();

                        Debug.Log($"Successfully loaded sbyte array {filename}");
                        onComplete?.Invoke(array);
                    }
                    catch (Exception e)
                    {
                        Debug.LogError($"Error deserializing sbyte array {filename}: {e.Message}");
                        onComplete?.Invoke(new sbyte[1]); // Return a placeholder array
                    }
                }
            }
        }

        // https://stackoverflow.com/questions/3695163/filestream-and-creating-folders
        // Joe: https://stackoverflow.com/users/13087/joe

        static void EnsureFolder(string path)
        {
            string directoryName = Path.GetDirectoryName(path);
            // If path is a file name only, directory name will be an empty string
            if (directoryName.Length > 0)
            {
                // Create all directories on the path that don't already exist
                Directory.CreateDirectory(directoryName);
            }
        }
    }    
}
