
## 1. Lista general de requerimientos

El sistema de OficioYa tiene los siguientes requerimientos para el dominio Worker:

### 1.1 Requerimientos funcionales

El sistema de OficioYa debe tener la capacidad de:

1. Gestionar el catálogo de oficios y sus categorías (crear, editar, listar, activar/desactivar). Las 11 categorías iniciales se cargan al iniciar el sistema.
2. Asociar a un trabajador su oficio principal y sus oficios secundarios, visibles en su perfil.
3. Establecer la tarifa de un trabajador por cada oficio que ofrece.
4. Consultar las zonas de cobertura (barrios) que define el dominio Users.
5. Permitir al trabajador seleccionar su área de cobertura entre esas zonas.
6. Permitir al trabajador especificar su disponibilidad semanal y el estado "disponible ahora".
7. Permitir al contratante consultar el perfil de un trabajador (oficios, tarifa, cobertura, disponibilidad).
8. Exponer una consulta paginada de trabajadores para que otros dominios puedan filtrarlos por oficio, zona, tarifa y disponibilidad (fecha y hora).
9. Configurar disponibilidad fija o repetitiva.
10. Permitir a otros dominios reservar y liberar una franja de disponibilidad de un trabajador, evitando reservas duplicadas.
11. Permitir al trabajador gestionar su galería de trabajos anteriores.
12. Permitir al trabajador consultar su propio perfil y saber si ya lo completó.

### 1.2 Requerimientos no funcionales

El sistema de OficioYa debe tener:

1. Documentar cómo otros dominios pueden consultar la información de trabajadores, para que si algo cambia en Worker, no se rompa lo que usan otros equipos.
2. Poder agregar nuevos oficios o categorías fácilmente, sin rediseñar el sistema.
3. Los formularios para registrar oficios, tarifa y cobertura deben ser fáciles de usar para el usuario. 
4. Los campos obligatorios se identifican con asterisco (*) y la tarifa se muestra con el símbolo de pesos ($).
5. Cobertura de pruebas unitarias mínima del 80%. Solo se excluye el código generado (Lombok y las clases `*MapperImpl` de MapStruct) y la clase `WorkerApplication`, y esas exclusiones quedan documentadas.
6. Toda la interfaz debe ser responsive.
7. El sistema debe registrar logs de todas las operaciones (no solo búsquedas), con rotación de archivos.
8. Todos los endpoints deben estar protegidos: exigen un token JWT válido y verifican el rol del usuario (TRABAJADOR, CONTRATANTE o ADMIN) según la operación.
9.  Debe existir una colección de Postman que incluya todos los endpoints del dominio Worker.

## 2. Diagramas de caso de uso

### 2.1 Requerimiento Funcional 1

| Campo | Descripción |
|------|-------------|
| **ID** | RF-01 |
| **Nombre del requerimiento** | Gestionar catálogo de oficios y categorías |
| **Descripción** | El sistema debe permitir al administrador crear, editar, listar y activar/desactivar los oficios del catálogo general, organizados en categorías. Al iniciar, el sistema carga las 11 categorías con sus oficios: Plomería y gas, Electricidad, Construcción y remodelación, Carpintería y muebles, Cerrajería y seguridad, Instalaciones y armado, Electrodomésticos, Aseo y limpieza, Jardinería y exteriores, Cuidado de mascotas y Servicios generales. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un usuario con rol Administrador autenticado y el módulo del dominio Worker disponible. |
| **Actor** | Administrador |
| **Flujo principal** | 1. El administrador accede al módulo de catálogo de oficios.<br>2. El sistema muestra las categorías y sus oficios.<br>3. El administrador selecciona crear, editar o activar/desactivar un oficio, o editar una categoría.<br>4. El sistema valida los datos ingresados (nombre único, campos obligatorios).<br>5. El sistema guarda el cambio y actualiza el listado. Un oficio desactivado deja de estar disponible para nuevas asociaciones. |
| **Diagrama de caso de uso** | <img width="616" height="195" alt="image" src="https://github.com/user-attachments/assets/79345334-5929-4716-9bae-f42a9e1c5695" />|
| **Poscondiciones** | El catálogo de oficios queda actualizado y los cambios están disponibles para los trabajadores y las consultas de otros dominios. |

### 2.2 Requerimiento Funcional 2

| Campo | Descripción |
|------|-------------|
| **ID** | RF-02 |
| **Nombre del requerimiento** | Asociar oficio principal y oficios secundarios al trabajador |
| **Descripción** | El sistema debe permitir asociar a un trabajador un oficio principal y uno o varios oficios secundarios tomados del catálogo. La gestión de oficios hace parte del perfil del trabajador. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un trabajador registrado y autenticado, y oficios existentes en el catálogo (RF-01). |
| **Actor** | Trabajador |
| **Flujo principal** | 1. El trabajador accede a su perfil y a la sección de oficios dentro de este.<br>2. El sistema muestra el catálogo de oficios disponibles.<br>3. El trabajador selecciona su oficio principal.<br>4. El trabajador selecciona uno o más oficios secundarios.<br>5. El sistema valida que exista un único oficio principal y que no haya duplicados.<br>6. El sistema guarda la asociación y la muestra en el perfil. |
| **Diagrama de caso de uso** | <img width="613" height="205" alt="image" src="https://github.com/user-attachments/assets/a5c15329-bcc9-428a-a92d-8359387d0585" />|
| **Poscondiciones** | El trabajador queda asociado a un oficio principal y a sus oficios secundarios, visibles en su perfil. |

### 2.3 Requerimiento Funcional 3

| Campo | Descripción |
|------|-------------|
| **ID** | RF-03 |
| **Nombre del requerimiento** | Establecer tarifa por oficio |
| **Descripción** | El sistema debe permitir al trabajador establecer su tarifa, en pesos colombianos (COP), por cada oficio que ofrece. La tarifa se guarda en la relación trabajador-oficio y otros dominios la consultan. Worker no maneja pagos ni ganancias. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un trabajador autenticado con al menos un oficio asociado (RF-03). |
| **Actor** | Trabajador |
| **Flujo principal** | 1. El trabajador accede a los oficios de su perfil.<br>2. El sistema lista los oficios asociados al trabajador.<br>3. El trabajador establece o modifica la tarifa de cada oficio.<br>4. El sistema valida que el valor sea numérico y mayor que cero.<br>5. El sistema guarda las tarifas. |
| **Diagrama de caso de uso** | <img width="620" height="157" alt="image" src="https://github.com/user-attachments/assets/2d765c2d-c48b-4ef2-9ea3-1f72f18e3c3c" />|
| **Poscondiciones** | Cada oficio del trabajador queda con su tarifa establecida, visible en su perfil y consultable por otros dominios. |

### 2.4 Requerimiento Funcional 4 

| Campo | Descripción |
|------|-------------|
| **ID** | RF-04 |
| **Nombre del requerimiento** | Consultar zonas de cobertura del dominio Users |
| **Descripción** | El sistema debe consultar al dominio Users la lista de zonas (barrios) disponibles, que se identifican por un id numérico. Worker no crea ni modifica zonas. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, el dominio Users debe estar disponible y tener zonas (barrios) registradas. |
| **Actor** | Sistema |
| **Flujo principal** | 1. El sistema solicita al dominio Users la lista de zonas (barrios) con sus ids.<br>2. El sistema valida los ids de zona recibidos del trabajador comparando cuántos envió con cuántos devolvió Users. Los ids inexistentes no aparecen.<br>3. Si falta alguno, el sistema rechaza la solicitud e indica que hay zonas inválidas. |
| **Diagrama de caso de uso** | <img width="628" height="205" alt="image" src="https://github.com/user-attachments/assets/dd1577cc-fffc-4c37-ac6c-9fbe600eab49" />|
| **Poscondiciones** | Solo se registran en el área de cobertura del trabajador zonas que existen en el dominio Users. |

### 2.5 Requerimiento Funcional 5

| Campo | Descripción |
|------|-------------|
| **ID** | RF-05 |
| **Nombre del requerimiento** | Definir área de cobertura del trabajador |
| **Descripción** | El sistema debe permitir al trabajador definir su área de cobertura seleccionando entre las zonas del dominio Users. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un trabajador autenticado y zonas de cobertura definidas (RF-05). |
| **Actor** | Trabajador |
| **Flujo principal** | 1. El trabajador accede a la sección de cobertura de su perfil.<br>2. El sistema muestra las zonas (barrios) disponibles.<br>3. El trabajador selecciona las zonas donde ofrece sus servicios.<br>4. El sistema valida que se haya seleccionado al menos una zona.<br>5. El sistema guarda el área de cobertura. |
| **Diagrama de caso de uso** | <img width="602" height="180" alt="image" src="https://github.com/user-attachments/assets/0b0b1d9f-e785-45be-b00c-4ea9eaa728c5" />|
| **Poscondiciones** | El área de cobertura del trabajador queda registrada y disponible para ser consultada y filtrada. |

### 2.6 Requerimiento Funcional 6

| Campo | Descripción |
|------|-------------|
| **ID** | RF-06 |
| **Nombre del requerimiento** | Gestionar disponibilidad semanal y estado "disponible ahora" |
| **Descripción** | El sistema debe permitir al trabajador especificar su disponibilidad semanal (días y franjas horarias) y activar o desactivar el estado "disponible ahora". |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un trabajador registrado y autenticado. |
| **Actor** | Trabajador |
| **Flujo principal** | 1. El trabajador accede a la sección de disponibilidad de su perfil.<br>2. El trabajador define los días y franjas horarias en los que trabaja.<br>3. El trabajador activa o desactiva el estado "disponible ahora".<br>4. El sistema valida que las franjas horarias sean coherentes (sin solapamientos ni rangos inválidos).<br>5. El sistema guarda la disponibilidad y el estado. |
| **Diagrama de caso de uso** | <img width="602" height="190" alt="image" src="https://github.com/user-attachments/assets/3cb34b3b-1fb6-4084-be0c-bd25e817a0e2" />|
| **Poscondiciones** | La disponibilidad semanal y el estado "disponible ahora" del trabajador quedan actualizados y visibles en su perfil. |

### 2.7 Requerimiento Funcional 7

| Campo | Descripción |
|------|-------------|
| **ID** | RF-07 |
| **Nombre del requerimiento** | Consultar perfil de un trabajador |
| **Descripción** | El sistema debe permitir al contratante consultar el perfil de un trabajador, incluyendo sus oficios, tarifa, cobertura y disponibilidad. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente al menos un trabajador con perfil registrado y un contratante que acceda a la plataforma. |
| **Actor** | Contratante |
| **Flujo principal** | 1. El contratante selecciona un trabajador.<br>2. El sistema recupera la información del perfil (oficios, tarifa, cobertura y disponibilidad).<br>3. El sistema muestra el perfil completo al contratante. |
| **Diagrama de caso de uso** | <img width="615" height="195" alt="image" src="https://github.com/user-attachments/assets/284e0286-8b5b-47b2-a8c9-55182f26f163" />|
| **Poscondiciones** | El contratante visualiza la información vigente del trabajador para decidir si lo contrata. |

### 2.8 Requerimiento Funcional 8

| Campo | Descripción |
|------|-------------|
| **ID** | RF-08 |
| **Nombre del requerimiento** | Exponer consulta de trabajadores a otros dominios |
| **Descripción** | El sistema debe exponer una consulta paginada de trabajadores que permita a otros dominios filtrarlos por oficio, zona (id de zona de Users), tarifa y disponibilidad (fecha y hora). |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente trabajadores registrados con oficios, tarifa, cobertura y disponibilidad definidos (RF-03, RF-04, RF-06, RF-07). |
| **Actor** | Dominio consumidor (otros dominios del sistema) |
| **Flujo principal** | 1. El dominio consumidor envía una consulta indicando uno o más filtros (oficio, zona, tarifa, disponibilidad) y la página solicitada.<br>2. El sistema valida los parámetros recibidos.<br>3. El sistema busca los trabajadores que cumplen los filtros.<br>4. El sistema retorna la página de trabajadores coincidentes. |
| **Diagrama de caso de uso** | <img width="500" height="132" alt="Requerimiento9" src="https://github.com/user-attachments/assets/5eeb028c-0594-4641-906b-207575f5ad1e" />|
| **Poscondiciones** | El dominio consumidor recibe la lista de trabajadores que cumplen los criterios de búsqueda. |

### 2.9 Requerimiento Funcional 9

| Campo | Descripción |
|------|-------------|
| **ID** | RF-10 |
| **Nombre del requerimiento** | Configurar disponibilidad fija o repetitiva |
| **Descripción** | El sistema debe permitir al trabajador registrar su disponibilidad como una fecha fija puntual o como un patrón repetitivo (ej. todos los lunes) válido durante un periodo específico, de modo que no tenga que volver a registrarla cada semana a menos que quiera cambiarla. |
| **Precondiciones** | 	Para que el sistema cumpla con este requerimiento, el trabajador debe tener su perfil profesional creado (RF-07 ya definido). |
| **Actor** | Trabajador |
| **Flujo principal** | 1. El actor elige si la disponibilidad que va a registrar es fija (un solo día) o repetitiva (un día de la semana). 2. Si es fija, el actor ingresa la fecha, hora de inicio y hora de fin. 3. Si es repetitiva, el actor ingresa el día de la semana, la hora de inicio y fin, y el periodo (fecha de inicio y, opcionalmente, fecha de fin) durante el cual aplica la repetición. 4. El sistema guarda la disponibilidad sin requerir que el trabajador la vuelva a registrar cada semana. 5. El trabajador puede, en cualquier momento, agregar una excepción puntual (ej. un lunes específico distinto a su patrón habitual). |
| **Diagrama de caso de uso** | aun no |
| **Poscondiciones** | Se espera como resultado que la disponibilidad del trabajador quede reflejada correctamente ya sea como una fecha única o como un patrón recurrente vigente dentro del periodo definido, sin necesidad de actualización manual semanal. |

### 2.10 Requerimiento Funcional 10

| Campo | Descripción |
|------|-------------|
| **ID** | RF-11 |
| **Nombre del requerimiento** | Reservar y liberar franja de disponibilidad |
| **Descripción** | El sistema debe permitir a otros dominios reservar una franja horaria de un trabajador para una fecha específica al crear una solicitud, y liberarla si la solicitud se rechaza, expira o se cancela. Una misma franja no puede reservarse dos veces para la misma fecha. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un trabajador con disponibilidad definida (RF-07, RF-10) y un dominio consumidor autenticado con permiso para reservar. |
| **Actor** | Dominio consumidor (Services / Orquestador) |
| **Flujo principal** | 1. El dominio consumidor envía el trabajador, la franja y la fecha a reservar.<br>2. El sistema valida que la franja exista y pertenezca al trabajador.<br>3. El sistema verifica que la franja esté libre en esa fecha. Si ya está ocupada, responde con un conflicto y no reserva.<br>4. Si está libre, el sistema la marca como ocupada para esa fecha.<br>5. Para liberarla, el dominio consumidor indica la reserva y el sistema la elimina. |
| **Diagrama de caso de uso** | aun no |
| **Poscondiciones** | La franja queda ocupada (o libre, si se liberó) y deja de aparecer (o vuelve a aparecer) en las búsquedas por disponibilidad de esa fecha. |

### 2.11 Requerimiento Funcional 11

| Campo | Descripción |
|------|-------------|
| **ID** | RF-11 |
| **Nombre del requerimiento** | Gestionar galería de trabajos anteriores |
| **Descripción** | El sistema debe permitir al trabajador agregar y eliminar fotos de sus trabajos anteriores en su galería. Las imágenes se almacenan en la base de datos de fotos gestionada por el Orquestador; Worker no guarda archivos y solo registra el identificador (`imageId`) y la URL de cada imagen. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un trabajador registrado y autenticado, y el servicio de almacenamiento de fotos del Orquestador disponible. |
| **Actor** | Trabajador |
| **Flujo principal** | 1. El trabajador accede a la galería de su perfil.<br>2. El sistema muestra las fotos existentes.<br>3. El trabajador sube una imagen al almacenamiento de fotos del Orquestador, que retorna su identificador y su URL.<br>4. El sistema de Worker registra en la galería ese identificador y esa URL.<br>5. Para eliminar, el trabajador selecciona una foto y el sistema quita su referencia de la galería.<br>6. El sistema valida que la foto pertenezca al trabajador y actualiza la galería. |
| **Diagrama de caso de uso** | aun no |
| **Poscondiciones** | La galería del trabajador queda actualizada y visible en su perfil. |

### 2.12 Requerimiento Funcional 12

| Campo | Descripción |
|------|-------------|
| **ID** | RF-12 |
| **Nombre del requerimiento** | Consultar el perfil propio de trabajador |
| **Descripción** | El sistema debe permitir al trabajador autenticado consultar su propio perfil y, si aún no lo ha creado, informarlo para que pueda completarlo. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un usuario autenticado con rol TRABAJADOR. |
| **Actor** | Trabajador |
| **Flujo principal** | 1. El trabajador autenticado solicita su perfil.<br>2. El sistema identifica al trabajador por el usuario del token.<br>3. Si existe el perfil, el sistema lo retorna.<br>4. Si no existe, el sistema informa que el perfil no existe, para que se le lleve a completarlo. |
| **Diagrama de caso de uso** | aun no |
| **Poscondiciones** | El trabajador recibe su perfil, o la indicación de que debe completarlo. |