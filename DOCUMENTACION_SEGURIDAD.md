# Documentación de Seguridad - Minimarket API

---

## 1. Análisis del Caso: Amenazas Identificadas y Estrategia Seleccionada

### Amenazas Identificadas

| Amenaza | Descripción | Impacto |
|---------|-------------|---------|
| **Acceso no autorizado a endpoints** | Usuarios sin autenticación acceden a recursos protegidos | Alto |
| **Escalación de privilegios** | Usuario con rol limitado accede a funciones administrativas | Alto |
| **Intercepción de credenciales** | Contraseñas enviadas en texto plano | Alto |
| **Robo de sesión** | Secuestro de sesión mediante cookies o tokens robados | Medio |
| **Ataques CSRF** | Solicitudes cruzadas maliciosas aprovechando sesiones activas | Medio |
| **Inyección de tokens falsos** | Tokens JWT fraudulentos generados sin la clave secreta | Alto |
| **Fuga de información** | Exposición de datos sensibles como passwords o roles | Medio |

### Estrategia Seleccionada: JWT + BCrypt + Autorización por Roles

Se optó por un esquema de **autenticación stateless basado en JWT** por las siguientes razones:

1. **Escalabilidad:** Al ser stateless, no requiere almacenamiento en servidor, facilitando horizontal scaling.
2. **Seguridad de credenciales:** BCrypt con factor de trabajo asegura que las contraseñas almacenadas son resistentes a ataques de fuerza bruta.
3. **Control de acceso granular:** Los roles (CLIENTE, EMPLEADO, ADMINISTRADOR) permiten aplicar el principio de mínimo privilegio mediante `@PreAuthorize`.
4. **Estandarización:** JWT es un estándar abierto (RFC 7519) con amplio soporte en la industria.

---

## 2. Guía de Configuración

### 2.1 Dependencias (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

### 2.2 Propiedades (application.properties)

```properties
jwt.secret=MiClaveSecretaSuperSeguraParaMinimarket2024TokenJWT
jwt.expiration=86400000
```

- `jwt.secret`: Clave secreta HMAC-SHA para firmar/verificar tokens.
- `jwt.expiration`: Tiempo de vida del token en milisegundos (24 horas).

### 2.3 Configuración de Seguridad (SecurityConfig.java)

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 2.4 JwtUtil - Generación y Validación de Tokens

- `generateToken(username, roles)`: Crea un token JWT con subject (username) y claims (roles).
- `extractUsername(token)`: Extrae el username del token.
- `extractRoles(token)`: Extrae los roles del token.
- `isTokenValid(token)`: Verifica que el token tenga firma válida y no esté expirado.

### 2.5 JwtAuthenticationFilter

Filtro que intercepta cada solicitud HTTP:
1. Extrae el token del header `Authorization: Bearer <token>`.
2. Valida el token usando `JwtUtil`.
3. Si es válido, carga el `UserDetails` y lo establece en el `SecurityContext`.
4. Si no hay token o es inválido, la solicitud continúa (será rechazada por el endpoint si requiere autenticación).

### 2.6 Autenticación (AuthController)

- **POST /api/auth/login**: Recibe `{ "username": "...", "password": "..." }`, autentica contra la base de datos y retorna un JWT.
- **POST /api/auth/register**: Registra un nuevo usuario con rol CLIENTE por defecto.

### 2.7 Roles y Acceso

| Rol | Endpoints Permitidos |
|-----|---------------------|
| **CLIENTE** | Productos (GET), Carrito (CRUD), Ventas (POST) |
| **EMPLEADO** | Todo lo de CLIENTE + Categorías, Inventario, DetalleVentas, Ventas (GET) |
| **ADMINISTRADOR** | Todo lo anterior + Usuarios (CRUD completo) |

### 2.8 Usuarios por Defecto (DataInitializer)

| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| `admin` | `admin123` | ADMINISTRADOR |
| `empleado` | `empleado123` | EMPLEADO |
| `cliente` | `cliente123` | CLIENTE |

---

## 3. Explicación Técnica

### 3.1 Protección contra Acceso No Autorizado

**Mecanismo:** El `JwtAuthenticationFilter` se ejecuta antes de que cualquier solicitud llegue a los controladores. Verifica la presencia y validez del token JWT en el header `Authorization`. Sin un token válido, el `SecurityContext` queda vacío y Spring Security rechaza la solicitud con HTTP 401.

**Flujo:**
```
Cliente -> Header: Bearer <token> -> JwtAuthenticationFilter -> SecurityContext -> Controller
```

### 3.2 Protección contra Escalación de Privilegios

**Mecanismo:** Se utiliza `@PreAuthorize` con expresiones como `hasRole('ADMINISTRADOR')` y `hasAnyRole('EMPLEADO', 'ADMINISTRADOR')`. Spring Security evalúa estos guards antes de ejecutar el método del controlador. Si el usuario autenticado no tiene el rol requerido, recibe HTTP 403 Forbidden.

**Ejemplos:**
```java
@PreAuthorize("hasRole('ADMINISTRADOR')")    // Solo administradores
@PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR')")  // Empleados y admins
@PreAuthorize("hasAnyRole('CLIENTE', 'EMPLEADO', 'ADMINISTRADOR')")  // Todos autenticados
```

### 3.3 Protección de Contraseñas (BCrypt)

**Mecanismo:** BCryptPasswordEncoder implementa el algoritmo bcrypt con salt incorporado. Cada contraseña se almacena como un hash del formato `$2a$10$...` donde `10` es el factor de trabajo. Esto hace que:
- Dos usuarios con la misma contraseña tengan hashes diferentes (por el salt).
- Ataques de rainbow table sean inviables.
- Ataques de fuerza bruta sean lentos (el factor de trabajo incrementa el tiempo de cómputo).

### 3.4 Protección contra Tokens Falsificados (JWT)

**Mecanismo:** Los tokens JWT están firmados con HMAC-SHA256 usando una clave secreta (`jwt.secret`). Sin conocer esta clave, un atacante no puede generar tokens válidos. `JwtUtil.parser().verifyWith(secretKey)` verifica la firma en cada solicitud.

### 3.5 Protección contra CSRF

**Mecanismo:** Al usar `SessionCreationPolicy.STATELESS`, la aplicación no crea sesiones en el servidor. Sin sesiones, los ataques CSRF (que dependen de cookies de sesión) no son aplicables. Por eso se deshabilita CSRF (`csrf.disable()`).

### 3.6 Protección contra Intercepción

**Recomendación:** En producción, toda la comunicación debe hacerse exclusivamente por HTTPS para evitar que tokens y credenciales sean interceptados en tránsito (ataques Man-in-the-Middle).

---

## 4. Pruebas Funcionales

### 4.1 Obtener Token (Login)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Respuesta:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "roles": ["ROLE_ADMINISTRADOR"]
}
```

### 4.2 Acceder a Endpoint Protegido

```bash
curl -X GET http://localhost:8080/api/usuarios \
  -H "Authorization: Bearer <token>"
```

### 4.3 Verificación de Roles

**Cliente intentando acceder a usuarios (debe fallar con 403):**
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"cliente","password":"cliente123"}' | jq -r '.token')
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/usuarios \
  -H "Authorization: Bearer $TOKEN"
# Respuesta: 403
```

**Admin accediendo a usuarios (debe funcionar):**
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')
curl http://localhost:8080/api/usuarios \
  -H "Authorization: Bearer $TOKEN"
# Respuesta: 200 OK con lista de usuarios
```

### 4.4 Sin Token (debe fallar con 401)

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/productos
# Respuesta: 401
```
