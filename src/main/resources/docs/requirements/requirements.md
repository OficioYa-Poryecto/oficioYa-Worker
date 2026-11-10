
## 1. Lista general de requerimientos

El sistema de OficioYa tiene los siguientes requerimientos para el dominio Worker:

### 1.1 Requerimientos funcionales

El sistema de OficioYa debe tener la capacidad de:

1. Gestionar el catálogo general de oficios (crear, editar, listar, eliminar).
2. Definir categorías especiales de oficios con campos propios (profesores, cuidadores de mascotas, técnicos).
3. Asociar a un trabajador su oficio principal y sus oficios secundarios.
4. Registrar la tarifa aproximada de un trabajador por cada oficio que ofrece.
5. Permitir al trabajador definir su área de cobertura dentro de esas zonas.
6. Permitir al trabajador especificar su disponibilidad semanal y el estado "disponible ahora".
7. Permitir al contratante consultar el perfil de un trabajador (oficios, tarifa, cobertura, disponibilidad).
8. Exponer una consulta de trabajadores para que otros dominios puedan filtrarlos por oficio, zona, tarifa y disponibilidad.
9. Configurar disponibilidad fija o repetitiva

### 1.2 Requerimientos no funcionales

El sistema de OficioYa debe tener:

1. Documentar cómo otros dominios pueden consultar la información de trabajadores, para que si algo cambia en Worker, no se rompa lo que usan otros equipos.
2. Se debe poder agregar nuevos oficios o categorías fácilmente sin tener que rediseñar todo el sistema cada vez que se necesite uno nuevo.
3. Las consultas que hacen otros dominios deben responde sin demoras que afecten la experiencia de búsqueda.
4. El formulario para registrar oficios, tarifa y cobertura debe ser fácil de usar.
5. Cobertura de pruebas unitarias minima del 80%.
6. La interfaz de búsqueda debe ser responisve.
7. El sistema debe registrar logs de cada búsqueda realizada.

## 2. Diagramas de caso de uso

### 2.1 Requerimiento Funcional 1

| Campo | Descripción |
|------|-------------|
| **ID** | RF-01 |
| **Nombre del requerimiento** | Gestionar catálogo de oficios |
| **Descripción** | El sistema debe permitir crear, editar, listar y eliminar los oficios que conforman el catálogo general de la plataforma. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un usuario con rol Administrador autenticado y el módulo del dominio Worker disponible. |
| **Actor** | Administrador |
| **Flujo principal** | 1. El administrador accede al módulo de catálogo de oficios.<br>2. El sistema muestra el listado de oficios existentes.<br>3. El administrador selecciona crear, editar o eliminar un oficio.<br>4. El sistema valida los datos ingresados (nombre único, campos obligatorios).<br>5. El sistema guarda el cambio y actualiza el listado. |
| **Diagrama de caso de uso** | <img width="616" height="195" alt="image" src="https://github.com/user-attachments/assets/79345334-5929-4716-9bae-f42a9e1c5695" />|
| **Poscondiciones** | El catálogo de oficios queda actualizado y los cambios están disponibles para los trabajadores y las consultas de otros dominios. |

### 2.2 Requerimiento Funcional 2

| Campo | Descripción |
|------|-------------|
| **ID** | RF-02 |
| **Nombre del requerimiento** | Definir categorías especiales de oficios |
| **Descripción** | El sistema debe permitir definir categorías especiales de oficios (profesores, cuidadores de mascotas, técnicos) con campos propios y adicionales a los de un oficio general. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente el catálogo de oficios (RF-01) y un usuario Administrador autenticado. |
| **Actor** | Administrador, Trabajador|
| **Flujo principal** | 1. El administrador selecciona un oficio o categoría del catálogo.<br>2. El administrador marca el oficio como categoría especial.<br>3. El administrador define los campos propios de la categoría (por ejemplo: materia y nivel para profesores; tipo de mascota para cuidadores; especialidad para técnicos).<br>4. El sistema valida la configuración de los campos.<br>5. El sistema guarda la categoría con su esquema de campos. |
| **Diagrama de caso de uso** |<img width="706" height="300" alt="image" src="https://github.com/user-attachments/assets/87bb5562-ddc8-4854-8e3a-81d773a72edd" />|
| **Poscondiciones** | La categoría especial queda registrada y el formulario del trabajador solicita los campos propios cuando este seleccione un oficio de esa categoría. |

### 2.3 Requerimiento Funcional 3

| Campo | Descripción |
|------|-------------|
| **ID** | RF-03 |
| **Nombre del requerimiento** | Asociar oficio principal y oficios secundarios al trabajador |
| **Descripción** | El sistema debe permitir asociar a un trabajador un oficio principal y uno o varios oficios secundarios tomados del catálogo. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un trabajador registrado y autenticado, y oficios existentes en el catálogo (RF-01). |
| **Actor** | Trabajador |
| **Flujo principal** | 1. El trabajador accede a la sección de oficios de su perfil.<br>2. El sistema muestra el catálogo de oficios disponibles.<br>3. El trabajador selecciona su oficio principal.<br>4. El trabajador selecciona uno o más oficios secundarios.<br>5. El sistema valida que exista un único oficio principal y que no haya duplicados.<br>6. El sistema guarda la asociación. |
| **Diagrama de caso de uso** | <img width="613" height="205" alt="image" src="https://github.com/user-attachments/assets/a5c15329-bcc9-428a-a92d-8359387d0585" />|
| **Poscondiciones** | El trabajador queda asociado a un oficio principal y a sus oficios secundarios, visibles en su perfil. |

### 2.4 Requerimiento Funcional 4

| Campo | Descripción |
|------|-------------|
| **ID** | RF-04 |
| **Nombre del requerimiento** | Registrar tarifa aproximada por oficio |
| **Descripción** | El sistema debe permitir registrar la tarifa aproximada del trabajador por cada oficio que ofrece. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un trabajador autenticado con al menos un oficio asociado (RF-03). |
| **Actor** | Trabajador |
| **Flujo principal** | 1. El trabajador accede a la sección de tarifas de su perfil.<br>2. El sistema lista los oficios asociados al trabajador.<br>3. El trabajador ingresa o modifica la tarifa aproximada para cada oficio.<br>4. El sistema valida que el valor sea numérico y mayor que cero.<br>5. El sistema guarda las tarifas. |
| **Diagrama de caso de uso** | <img width="620" height="157" alt="image" src="https://github.com/user-attachments/assets/2d765c2d-c48b-4ef2-9ea3-1f72f18e3c3c" />|
| **Poscondiciones** | Cada oficio del trabajador queda con su tarifa aproximada registrada y visible en su perfil. |

### 2.5 Requerimiento Funcional 5

| Campo | Descripción |
|------|-------------|
| **ID** | RF-05 |
| **Nombre del requerimiento** | Definir zonas de cobertura de la plataforma |
| **Descripción** | El sistema debe permitir definir las zonas de cobertura (barrios) disponibles en la plataforma. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un usuario Administrador autenticado. |
| **Actor** | Administrador |
| **Flujo principal** | 1. El administrador accede al módulo de zonas de cobertura.<br>2. El sistema muestra las zonas (barrios) existentes.<br>3. El administrador crea, edita o elimina una zona.<br>4. El sistema valida los datos (nombre único, campos obligatorios).<br>5. El sistema guarda el cambio y actualiza el listado de zonas. |
| **Diagrama de caso de uso** | <img width="628" height="205" alt="image" src="https://github.com/user-attachments/assets/dd1577cc-fffc-4c37-ac6c-9fbe600eab49" />|
| **Poscondiciones** | El catálogo de zonas de cobertura queda actualizado y disponible para que los trabajadores definan su cobertura. |

### 2.6 Requerimiento Funcional 6

| Campo | Descripción |
|------|-------------|
| **ID** | RF-06 |
| **Nombre del requerimiento** | Definir área de cobertura del trabajador |
| **Descripción** | El sistema debe permitir al trabajador definir su área de cobertura seleccionando entre las zonas disponibles en la plataforma. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un trabajador autenticado y zonas de cobertura definidas (RF-05). |
| **Actor** | Trabajador |
| **Flujo principal** | 1. El trabajador accede a la sección de cobertura de su perfil.<br>2. El sistema muestra las zonas (barrios) disponibles.<br>3. El trabajador selecciona las zonas donde ofrece sus servicios.<br>4. El sistema valida que se haya seleccionado al menos una zona.<br>5. El sistema guarda el área de cobertura. |
| **Diagrama de caso de uso** | <img width="602" height="180" alt="image" src="https://github.com/user-attachments/assets/0b0b1d9f-e785-45be-b00c-4ea9eaa728c5" />|
| **Poscondiciones** | El área de cobertura del trabajador queda registrada y disponible para ser consultada y filtrada. |

### 2.7 Requerimiento Funcional 7

| Campo | Descripción |
|------|-------------|
| **ID** | RF-07 |
| **Nombre del requerimiento** | Gestionar disponibilidad semanal y estado "disponible ahora" |
| **Descripción** | El sistema debe permitir al trabajador especificar su disponibilidad semanal (días y franjas horarias) y activar o desactivar el estado "disponible ahora". |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente un trabajador registrado y autenticado. |
| **Actor** | Trabajador |
| **Flujo principal** | 1. El trabajador accede a la sección de disponibilidad de su perfil.<br>2. El trabajador define los días y franjas horarias en los que trabaja.<br>3. El trabajador activa o desactiva el estado "disponible ahora".<br>4. El sistema valida que las franjas horarias sean coherentes (sin solapamientos ni rangos inválidos).<br>5. El sistema guarda la disponibilidad y el estado. |
| **Diagrama de caso de uso** | <img width="602" height="190" alt="image" src="https://github.com/user-attachments/assets/3cb34b3b-1fb6-4084-be0c-bd25e817a0e2" />|
| **Poscondiciones** | La disponibilidad semanal y el estado "disponible ahora" del trabajador quedan actualizados y visibles en su perfil. |

### 2.8 Requerimiento Funcional 8

| Campo | Descripción |
|------|-------------|
| **ID** | RF-08 |
| **Nombre del requerimiento** | Consultar perfil de un trabajador |
| **Descripción** | El sistema debe permitir al contratante consultar el perfil de un trabajador, incluyendo sus oficios, tarifa, cobertura y disponibilidad. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente al menos un trabajador con perfil registrado y un contratante que acceda a la plataforma. |
| **Actor** | Contratante |
| **Flujo principal** | 1. El contratante selecciona un trabajador.<br>2. El sistema recupera la información del perfil (oficios, tarifa, cobertura y disponibilidad).<br>3. El sistema muestra el perfil completo al contratante. |
| **Diagrama de caso de uso** | <img width="615" height="195" alt="image" src="https://github.com/user-attachments/assets/284e0286-8b5b-47b2-a8c9-55182f26f163" />|
| **Poscondiciones** | El contratante visualiza la información vigente del trabajador para decidir si lo contrata. |

### 2.9 Requerimiento Funcional 9

| Campo | Descripción |
|------|-------------|
| **ID** | RF-09 |
| **Nombre del requerimiento** | Exponer consulta de trabajadores a otros dominios |
| **Descripción** | El sistema debe exponer una consulta de trabajadores que permita a otros dominios filtrarlos por oficio, zona, tarifa y disponibilidad. |
| **Precondiciones** | Para que el sistema cumpla con este requerimiento, OficioYa debe tener previamente trabajadores registrados con oficios, tarifa, cobertura y disponibilidad definidos (RF-03, RF-04, RF-06, RF-07). |
| **Actor** | Dominio consumidor (otros dominios del sistema) |
| **Flujo principal** | 1. El dominio consumidor envía una consulta indicando uno o más filtros (oficio, zona, tarifa, disponibilidad).<br>2. El sistema valida los parámetros recibidos.<br>3. El sistema busca los trabajadores que cumplen los filtros.<br>4. El sistema retorna la lista de trabajadores coincidentes. |
| **Diagrama de caso de uso** | <img width="500" height="132" alt="Requerimiento9" src="https://github.com/user-attachments/assets/5eeb028c-0594-4641-906b-207575f5ad1e" />
|
| **Poscondiciones** | El dominio consumidor recibe la lista de trabajadores que cumplen los criterios de búsqueda. |

### 2.10 Requerimiento Funcional 10

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


