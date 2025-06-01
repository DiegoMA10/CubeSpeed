using UnityEngine;
using System.Collections;
using Kociemba;

/// <summary>
/// Helper class to load Kociemba tables on Android.
/// Attach this to a GameObject in your scene to ensure tables are loaded before using the solver.
/// </summary>
public class KociembaTableLoader : MonoBehaviour
{
    [Tooltip("Set to true to load tables on Awake, false to load them manually")]
    public bool loadOnAwake = true;
    
    [Tooltip("Set to true to destroy this GameObject after loading, false to keep it")]
    public bool destroyAfterLoading = true;
    
    private bool isLoading = false;
    private bool hasLoaded = false;
    
    // Event that fires when tables are loaded
    public delegate void TablesLoadedEvent();
    public static event TablesLoadedEvent OnTablesLoaded;
    
    // Singleton instance
    private static KociembaTableLoader _instance;
    public static KociembaTableLoader Instance
    {
        get
        {
            if (_instance == null)
            {
                _instance = FindFirstObjectByType<KociembaTableLoader>();
                
                if (_instance == null)
                {
                    GameObject go = new GameObject("KociembaTableLoader");
                    _instance = go.AddComponent<KociembaTableLoader>();
                }
            }
            return _instance;
        }
    }
    
    private void Awake()
    {
        // Ensure we only have one instance
        if (_instance != null && _instance != this)
        {
            Destroy(gameObject);
            return;
        }
        
        _instance = this;
        
        // Don't destroy when loading new scenes
        DontDestroyOnLoad(gameObject);
        
        if (loadOnAwake)
        {
            LoadTables();
        }
    }
    
    /// <summary>
    /// Load the Kociemba tables if they haven't been loaded yet
    /// </summary>
    public void LoadTables()
    {
        if (!isLoading && !hasLoaded && !Tools.TablesLoaded)
        {
            StartCoroutine(LoadTablesCoroutine());
        }
        else if (Tools.TablesLoaded)
        {
            Debug.Log("Kociemba tables are already loaded.");
        }
    }
    
    /// <summary>
    /// Coroutine to load the tables
    /// </summary>
    private IEnumerator LoadTablesCoroutine()
    {
        isLoading = true;
        Debug.Log("Starting to load Kociemba tables...");
        
        yield return StartCoroutine(Tools.LoadTablesAsync(this, () => {
            hasLoaded = true;
            isLoading = false;
            
            // Notify listeners that tables are loaded
            OnTablesLoaded?.Invoke();
            
            Debug.Log("Kociemba tables loaded successfully!");
            
            if (destroyAfterLoading)
            {
                Destroy(gameObject);
            }
        }));
    }
    
    /// <summary>
    /// Check if tables are loaded
    /// </summary>
    public bool AreTablesLoaded()
    {
        return Tools.TablesLoaded;
    }
    
    /// <summary>
    /// Wait for tables to be loaded before continuing
    /// </summary>
    public IEnumerator WaitForTablesLoaded()
    {
        if (!Tools.TablesLoaded)
        {
            LoadTables();
            yield return new WaitUntil(() => Tools.TablesLoaded);
        }
    }
}