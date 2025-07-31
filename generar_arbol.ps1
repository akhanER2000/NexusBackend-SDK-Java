# ===================================================================
# Script para generar un árbol de directorios (Versión compatible)
# ===================================================================

# --- Definición de la función Show-Tree ---
function Show-Tree {
    param (
        [string]$Path,
        [string]$Indent = ""
    )

    # Obtenemos todos los archivos y carpetas del directorio
    # Usamos -ErrorAction SilentlyContinue para ignorar errores de acceso denegado
    $children = Get-ChildItem -LiteralPath $Path -ErrorAction SilentlyContinue
    if (-not $children) { return } # Salir si la carpeta está vacía o no es accesible
    $lastChild = $children[-1]

    foreach ($child in $children) {
        # Preparamos los prefijos gráficos para las líneas del árbol (usando ASCII)
        if ($child -eq $lastChild) {
            $prefix = "\-- " # Reemplazo de '└──'
        } else {
            $prefix = "+-- " # Reemplazo de '├──'
        }

        # Escribimos la línea para el archivo/carpeta actual
        Write-Output "$Indent$prefix$($child.Name)"

        # Si el elemento es una carpeta, llamamos a la función de nuevo (recursividad)
        if ($child.PSIsContainer) {
            if ($child -eq $lastChild) {
                $newIndent = "    "
            } else {
                $newIndent = "|   " # Reemplazo de '│'
            }
            Show-Tree -Path $child.FullName -Indent ($Indent + $newIndent)
        }
    }
}

# --- Ejecución del script ---

# 1. Define la ruta del directorio actual del proyecto
$directorioActual = $PSScriptRoot

# 2. Define el nombre del archivo de salida
$archivoSalida = Join-Path -Path $directorioActual -ChildPath "arbol_directorios_SDK_4.txt"

# 3. Escribe la ruta del directorio raíz en el archivo (sobrescribiendo si ya existe)
Write-Output "Arbol de: $directorioActual" | Out-File -FilePath $archivoSalida -Encoding utf8

# 4. Ejecuta la función y añade el resultado al archivo
Show-Tree -Path $directorioActual | Out-File -FilePath $archivoSalida -Encoding utf8 -Append

# 5. Muestra un mensaje de confirmación en la consola
Write-Host "✅ Arbol de directorios guardado en '$archivoSalida'"
