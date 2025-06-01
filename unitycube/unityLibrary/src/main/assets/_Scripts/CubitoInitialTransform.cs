using UnityEngine;

public class Cubito : MonoBehaviour
{
    // Posición y rotación inicial
    [HideInInspector] public Vector3 posicionInicial;
    [HideInInspector] public Quaternion rotacionInicial;

    private void Awake()
    {
        // Guardar posición y rotación inicial al inicio
        posicionInicial = transform.localPosition;
        rotacionInicial = transform.localRotation;
    }
}