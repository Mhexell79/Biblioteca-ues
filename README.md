Biblioteca UES

Sistema de biblioteca universitaria de escritorio, hecho en Java con Swing y MongoDB. Proyecto de la asignatura Programación Orientada a Objetos (POO115), Ciclo II-2026, Universidad de El Salvador en Línea.

Permite administrar materiales (libros y revistas), usuarios y los préstamos y devoluciones, con inicio de sesión y contraseñas cifradas.

Tecnologías
Java 21
Java Swing (interfaz gráfica)
MongoDB (base de datos) con el driver mongodb-driver-sync 5.5.1
Maven
Git y GitHub
Requisitos
JDK 21 o superior
Maven 3.9 o superior (o NetBeans con soporte Maven)
MongoDB en ejecución (por defecto en mongodb://localhost:27017)
Configuración

La conexión se lee de variables de entorno. Si no existen, se usan estos valores:

Variable	Valor por defecto	Para qué sirve
MONGODB_URI	mongodb://localhost:27017	Dirección del servidor de MongoDB
MONGODB_DATABASE	biblioteca_ues	Nombre de la base de datos

La primera vez que se ejecuta con la base vacía, el programa crea:

un usuario administrador: admin@ues.edu.sv (su contraseña inicial está definida en el método inicializarSiVacio() de BibliotecaRepository);
tres materiales de ejemplo: dos libros y una revista. (MiClaveSegura123)
Cómo ejecutarlo

Desde la carpeta del proyecto:

Abrir el proyecto en NetBeans y ejecutar la clase com.biblioteca.app.Main.

Funciones

Inicio de sesión

Ingreso con correo y contraseña. La contraseña se cifra con SHA-256 antes de compararla.
Las cuentas inactivas no pueden entrar.

Materiales

Registrar libros (autor e ISBN) y revistas (número de edición y volumen), con editorial y cantidad de ejemplares.
Buscar por código, título, autor o tipo.
Consultar el catálogo con la cantidad disponible.
Modificar y eliminar. No se puede eliminar un material con préstamo activo ni con historial de préstamos.

Usuarios (pestaña visible solo para el personal: tipo Docente o Bibliotecario)

Registrar, buscar por código, documento o nombre, consultar y modificar.
Activar o inactivar, y eliminar. No se puede inactivar ni eliminar un usuario con préstamos activos, ni la cuenta con la que se inició sesión.

Préstamos y devoluciones

Validan que el usuario exista y esté activo y que el material tenga ejemplares disponibles.
El vencimiento es de 15 días para libros y 7 para revistas.
Al prestar baja en 1 la cantidad disponible, y al devolver sube en 1.
La tabla de préstamos muestra los préstamos activos y los devueltos.

Validaciones

Campos obligatorios, año entre 1450 y el año actual, cantidad entera y no negativa.
Correo con arroba, y códigos, correos y documentos únicos.
Estructura del proyecto
text
src/main/java/com/biblioteca/
  app/          Main.java                    Arranque de la aplicación
  config/       MongoConfig.java             Conexión con MongoDB
  modelo/       Material, Usuario, Prestamo  Clases de datos (record)
  repositorio/  BibliotecaRepository.java    Acceso y validación de datos
  servicio/     PasswordUtil.java            Hash SHA-256
  vista/        LoginFrame, BibliotecaFrame  Ventanas Swing

La vista nunca habla con MongoDB directamente: siempre pasa por BibliotecaRepository.

Base de datos

Base biblioteca_ues, con tres colecciones:

Colección	Campos
usuarios	_id, nombre, documento, contacto, email, tipo, passwordHash, activo
materiales	_id, tipo, titulo, anio, editorial, cantidadDisponible, autor, isbn, numeroEdicion, volumen
prestamos	_id, idUsuario, idMaterial, fecha, vence, activo
Pendientes conocidos
Búsqueda y filtros en la pestaña de préstamos (por código, usuario, material o estado) y consulta de préstamos vencidos.
Guardar la fecha efectiva de devolución.
Separar Material en Libro y Revista con una clase base.

Equipo
Integrante	GitHub
Henriquez Rodriguez, Marlon Hexell Alexander	Mhexell79
Menjivar Gonzalez, Camilo Alberto	miloo-z19
Pérez Ramos, Francisco Daniel	pr25052
Sandoval Tadeo, Carlos Antonio	Carlxs17

Docente tutor: Julio Cesar Brizuela Alvarado.
