# Grabador Universitario · 0.2 en línea

Graba y organiza clases sin conexión. La transcripción en línea usa Groq y requiere una clave personal introducida en Preferencias. Wi-Fi es el valor predeterminado; datos móviles se habilitan explícitamente.

Los archivos originales de 15 minutos se conservan. Cada envío reúne hasta tres segmentos AAC compatibles y hasta 24 MB estimados, sin recomprimir. Se comprueba el límite real de 25 MB antes de subir. Los bloques completados se guardan atómicamente y se reutilizan al reintentar si los nombres y contenidos originales no han cambiado. Una interrupción durante un bloque puede requerir repetir ese bloque; no se promete procesamiento exactamente una vez en el servidor.

La clave se cifra con Android Keystore y se guarda en noBackupFilesDir, fuera de copias Android y de las copias ZIP. Las peticiones HTTPS se vinculan a la red permitida. No se incluyen credenciales en el código ni se registra la clave.

## Primera prueba en teléfono

1. Instalar la actualización firmada sin desinstalar la versión anterior.
2. Abrir Mis grabaciones → Preferencias de transcripción.
3. Pegar la clave de una cuenta Groq en el plan gratuito y guardar. Mantener datos móviles desactivados para probar con Wi-Fi.
4. Grabar un minuto, detener y pulsar Transcribir. Confirmar el envío a Groq.
5. Ver transcripción completa y exportar TXT.
6. Probar una clase con varios segmentos: verificar continuidad en los cambios de segmento y recuperación tras desconectar Wi-Fi. Los límites 429 detienen el proceso; hay que reintentar cuando la cuota se renueve.

El estado indica bloques completados y la etapa de envío/procesamiento. No simula el porcentaje interno del servidor. Los límites exactos de una cuenta deben revisarse en Groq. La aplicación no puede verificar el plan de facturación de una clave; conservar el plan gratuito es necesario para costo obligatorio $0.00. Los datos móviles tienen el costo de tu operador.

Whisper local se conserva como opción experimental limitada a segmentos de tres minutos. El resumen automático no está implementado.

## Validación

La compilación ejecuta siete pruebas de agrupación, límites y espera; requiere comprobar en un teléfono Android el cifrado de claves, el envío real a Groq, la unión AAC, el visor y la actualización con la firma existente. La prueba real no puede hacerse sin la clave y el teléfono del usuario.

Fuentes: https://console.groq.com/docs/speech-to-text y https://console.groq.com/docs/rate-limits
