using System.Collections;
using System.Collections.Generic;
using UnityEngine;

/// <summary>
/// Controlador mejorado para la rotación del cubo y la cámara.
/// Proporciona una experiencia de cámara más intuitiva y fluida.
/// </summary>
public class RotateBigCube : MonoBehaviour
{
    [Header("Referencias")]
    public GameObject target; // Objeto que almacena la rotación objetivo del cubo

    [Header("Configuración de Rotación")]
    [Tooltip("Velocidad de rotación automática del cubo")]
    public float autoRotationSpeed = 300f;

    [Header("Configuración de Cámara")]
    [Tooltip("Distancia de la cámara al cubo")]
    public float cameraDistance = 10f;
    [Tooltip("Velocidad de rotación de la cámara")]
    public float cameraRotationSpeed = 10f;
    [Tooltip("Suavizado de movimiento de cámara (0-1)")]
    [Range(0f, 1f)]
    public float cameraSmoothness = 0.2f;

    // Variables privadas para la cámara
    private Camera mainCamera;
    private Vector2 orbitAngles = new Vector2(45f, 30f); // Initial isometric view
    private Vector2 targetOrbitAngles = new Vector2(45f, 30f); // Initial isometric view
    private Vector3 lastMousePosition;
    private bool isDragging = false;

    // Variables para comunicación con otros scripts
    public static bool isRotatingCube = false;
    public static bool isAutoRotating = false;
    public static bool rotationStartedOutsideCube = false;

    void Start()
    {
        // Inicializar referencias
        mainCamera = Camera.main;
        if (mainCamera == null)
        {
            Debug.LogError("No se encontró la cámara principal. Asegúrate de tener una cámara con tag 'MainCamera'.");
            enabled = false;
            return;
        }

        // Inicializar la posición de la cámara
        UpdateCameraPosition();

        // Mostrar instrucciones de control
        Debug.Log("Controles de cámara: \n" +
                  "- Arrastrar con el ratón/dedo: Rotar la cámara\n" +
                  "- R: Resetear la posición de la cámara");
    }

    void Update()
    {
        // No procesar input si se está rotando una cara del cubo
        if (RubikCrossPlatform.isRotatingFace)
        {
            return;
        }

        // Manejar input de usuario
        HandleUserInput();

        // Actualizar posición de la cámara con suavizado
        SmoothCameraUpdate();

        // Manejar rotación automática del cubo hacia el target
        HandleAutoRotation();
    }

    /// <summary>
    /// Maneja toda la entrada del usuario para la cámara y el cubo
    /// </summary>
    private void HandleUserInput()
    {
        // Detectar inicio de input (toque o click)
        if ((Input.touchCount > 0 && Input.GetTouch(0).phase == TouchPhase.Began) ||
            Input.GetMouseButtonDown(0))
        {
            // No permitir nuevas interacciones durante animación automática
            if (isAutoRotating)
            {
                return;
            }

            // Obtener posición de input
            Vector2 inputPosition = GetInputPosition();
            lastMousePosition = inputPosition;

            // Lanzar rayo para detectar si tocamos el cubo
            Ray ray = mainCamera.ScreenPointToRay(inputPosition);

            // Solo permitir rotación de cámara si NO tocamos el cubo inicialmente
            if (!Physics.Raycast(ray, out _))
            {
                isDragging = true;
                isRotatingCube = true;
                rotationStartedOutsideCube = true;
            }
            else
            {
                rotationStartedOutsideCube = false;
            }
        }

        // Manejar arrastre para rotación de cámara
        if (isDragging && isRotatingCube)
        {
            if ((Input.touchCount > 0 && Input.GetTouch(0).phase == TouchPhase.Moved) ||
                Input.GetMouseButton(0))
            {
                Vector2 currentPosition = GetInputPosition();
                Vector2 delta = currentPosition - (Vector2)lastMousePosition;

                // Actualizar ángulos objetivo (signos ajustados para movimiento más intuitivo)
                targetOrbitAngles.x += delta.x * cameraRotationSpeed * 0.01f;
                // Mantener el mismo signo para que la rotación vertical sea consistente
                targetOrbitAngles.y -= delta.y * cameraRotationSpeed * 0.01f;
                // ¡NUEVO!: Restringe el valor del ángulo vertical para evitar el flip:
                targetOrbitAngles.y = Mathf.Clamp(targetOrbitAngles.y, -85f, 85f);

                lastMousePosition = currentPosition;
            }
            // Finalizar arrastre
            else if ((Input.touchCount > 0 && Input.GetTouch(0).phase == TouchPhase.Ended) ||
                     Input.GetMouseButtonUp(0))
            {
                isDragging = false;
                isRotatingCube = false;
                rotationStartedOutsideCube = false;
            }
        }

        // Resetear cámara con tecla R
        if (Input.GetKeyDown(KeyCode.R))
        {
            ResetCamera();
        }
    }

    /// <summary>
    /// Actualiza la posición de la cámara con suavizado
    /// </summary>
    private void SmoothCameraUpdate()
    {
        // Aplicar suavizado a los ángulos de órbita
        orbitAngles = Vector2.Lerp(orbitAngles, targetOrbitAngles, cameraSmoothness);

        // Actualizar posición de la cámara
        UpdateCameraPosition();
    }

    /// <summary>
    /// Actualiza la posición de la cámara basada en los ángulos de órbita
    /// </summary>
    private void UpdateCameraPosition()
    {
        if (mainCamera == null) return;

        // Rotación básica tipo coordenadas esféricas
        float xRad = orbitAngles.x * Mathf.Deg2Rad;
        float yRad = orbitAngles.y * Mathf.Deg2Rad;

        float radius = cameraDistance;
        float horizontalFactor = Mathf.Cos(yRad);
        float x = radius * horizontalFactor * Mathf.Sin(xRad);
        float z = radius * horizontalFactor * Mathf.Cos(xRad);
        float y = radius * Mathf.Sin(yRad);

        mainCamera.transform.position = transform.position + new Vector3(x, y, z);

        // ¡Ojo! Sin cambiar el up por defecto
        mainCamera.transform.LookAt(transform.position, Vector3.up);
    }

    /// <summary>
    /// Maneja la rotación automática del cubo hacia su objetivo
    /// </summary>
    private void HandleAutoRotation()
    {
        if (target == null) return;

        // Si el cubo no está en la rotación objetivo, rotarlo suavemente
        if (!isRotatingCube && transform.rotation != target.transform.rotation)
        {
            // Marcar que estamos en animación automática
            isAutoRotating = true;

            // Calcular paso de rotación
            float step = autoRotationSpeed * Time.deltaTime;

            // Rotar suavemente hacia el objetivo
            transform.rotation = Quaternion.RotateTowards(transform.rotation, target.transform.rotation, step);

            // Si estamos muy cerca del objetivo, ajustar exactamente
            if (Quaternion.Angle(transform.rotation, target.transform.rotation) < 0.1f)
            {
                transform.rotation = target.transform.rotation;
                isAutoRotating = false;
            }
        }
        else if (transform.rotation == target.transform.rotation)
        {
            // Asegurar que se desactive la animación cuando ya está en posición
            isAutoRotating = false;
        }
    }

    /// <summary>
    /// Resetea la cámara a su posición y orientación por defecto
    /// </summary>
    private void ResetCamera()
    {
        targetOrbitAngles = new Vector2(45f, 30f); // Reset to isometric view
        orbitAngles = new Vector2(45f, 30f); // Reset to isometric view
        UpdateCameraPosition();
        Debug.Log("Cámara reseteada a la posición isométrica");
    }

    /// <summary>
    /// Obtiene la posición actual del input (touch o mouse)
    /// </summary>
    private Vector2 GetInputPosition()
    {
        if (Input.touchCount > 0)
            return Input.GetTouch(0).position;
        else
            return Input.mousePosition;
    }
}
