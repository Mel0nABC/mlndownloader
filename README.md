# Multi Thread Downloader (Java)

Proyecto educativo en Java para aprender cómo implementar un **gestor de descargas multihilo**, utilizando HTTP Range, procesamiento concurrente y verificación de integridad de archivos.

---

## 📌 Descripción

Este proyecto implementa un descargador de archivos que divide el contenido en múltiples partes y las descarga en paralelo usando hilos.

Incluye:

- Descarga por **HTTP Range**
- Descarga multihilo (`Thread`)
- Unión final de partes en un único archivo
- Verificación de tamaño final
- Verificación de checksum SHA-256 (opcional)

---

## 🚀 Funcionamiento

1. El usuario introduce una URL.
2. Se realiza una petición `HEAD` para obtener el tamaño del archivo.
3. El archivo se divide en múltiples partes (chunks).
4. Cada parte se descarga en paralelo usando threads.
5. Cada chunk se guarda temporalmente como:

```
archivo_PART_0
archivo_PART_1
archivo_PART_2
...
```

6. Al finalizar:
   - Se ordenan las partes
   - Se unen en un solo archivo final
   - Se eliminan los archivos temporales

---

## ⚙️ Tecnologías usadas

- Java 11+ (`HttpClient`)
- Threads nativos (`Thread`)
- Java NIO (`Files`, `Path`)
- Apache Commons Codec (SHA-256)
- ProcessBuilder (integración con comandos del sistema)

---

## 🧠 Conceptos aplicados

- Programación concurrente
- Descargas HTTP con `Range`
- Gestión de streams en Java
- Manipulación de archivos con NIO
- Procesamiento de checksum
- Ejecución de procesos del sistema operativo

---

## ▶️ Ejecución

Compilar:

```bash
javac dev/mel0n/App.java
```

Ejecutar:

```bash
java dev.mel0n.App
```

---

## 📥 Uso

Al iniciar el programa:

```
Por favor, indica el link para descargar:
```

Ejemplo:

```
https://example.com/archivo.iso
```

---

## 📊 Información mostrada

- Tamaño total del archivo
- Tamaño de cada chunk
- Número de partes
- Estado de descarga
- Tiempo total de descarga

---

## 🔐 Verificación de integridad

### Apache Commons Codec

```java
DigestUtils.sha256Hex(InputStream)
```

### Linux

```bash
sha256sum -c sha256sums.txt
```

---

## ⚠️ Limitaciones

- No incluye reintentos automáticos
- Depende de soporte HTTP Range del servidor
- No optimizado para producción

---

## 📚 Objetivo

Proyecto educativo para aprender:

- Concurrencia en Java
- Descarga paralela de archivos
- HTTP avanzado
- Manejo de streams y archivos
