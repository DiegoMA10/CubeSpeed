using UnityEngine;

public class AjustarFocalLengthPorOrientacion : MonoBehaviour
{
    public Camera cam;
    public float focalLengthLandscape = 25f; // Valor para landscape
    public float focalLengthPortrait = 50f;  // Valor para portrait (normal)

    void Start()
    {
        if (cam == null) cam = Camera.main;
        AjustarFocalLength();
    }

    void Update()
    {
        AjustarFocalLength();
    }

    void AjustarFocalLength()
    {
        if (!cam.usePhysicalProperties)
        {
            cam.usePhysicalProperties = true; // Activa la Physical Camera si no lo está
        }

        if (Screen.width > Screen.height)
        {
            // Landscape
            cam.focalLength = focalLengthLandscape;
        }
        else
        {
            // Portrait (normal)
            cam.focalLength = focalLengthPortrait;
        }
    }
}