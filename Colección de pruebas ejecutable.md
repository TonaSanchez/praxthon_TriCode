# Colección de pruebas ejecutables tipo Postman

##  A01 — T2T bien formada
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
Clave-Idempotencia: a01-{{$randomUUID}}

{
  "tipoOperacion": "T2T",
  "emisor": {
    "institucion": "801",
    "cuenta": "801180000000100016",
    "nombre": "Ana Ruiz Delgado"
  },
  "receptor": {
    "institucion": "802",
    "cuenta": "802180000000990010",
    "nombre": "Luis Cano Mora"
  },
  "importe": { "valor": 1500.50, "divisa": "MXN" },
  "concepto": "Pago de servicios",
  "folioNumerico": 200001,
  "referenciaSeguimiento": "PRUEBAA01001"
}

##  Resultado esperado
HTTP 201 Created

{
  "id": "op_N",
  "referenciaSeguimiento": "PRUEBAA01001",
  "estado": "RECIBIDO",
  "tipoOperacion": "T2T",
  "importe": { "valor": 1500.50, "divisa": "MXN" },
  "fechaRegistro": "...",
  "transiciones": [
    { "estado": "RECIBIDO", "momento": "...", "motivo": null }
  ]
}


##  A02 — VNT bien formada
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
Clave-Idempotencia: a01-{{$randomUUID}}

{
  "tipoOperacion": "VNT",
  "emisor": {
    "institucion": "801",
    "nombre": "Marta Solis Vega",
    "sucursal": "0417",
    "documentoIdentidad": { "tipo": "INE", "numero": "IDMEX1734558" }
  },
  "receptor": {
    "institucion": "803",
    "cuenta": "803180000000990020",
    "nombre": "Comercial Vega SA de CV"
  },
  "importe": { "valor": 3200.00, "divisa": "MXN" },
  "concepto": "Deposito en ventanilla",
  "folioNumerico": 200002,
  "referenciaSeguimiento": "PRUEBAA02001"
}

##  Resultado esperado
{
  "id": "op_N",
  "referenciaSeguimiento": "PRUEBAA02001",
  "estado": "RECIBIDO",
  "tipoOperacion": "VNT",
  "importe": { "valor": 3200.00, "divisa": "MXN" },
  "fechaRegistro": "...",
  "transiciones": [
    { "estado": "RECIBIDO", "momento": "...", "motivo": null }
  ]
}

##  A03 — T2T sin emisor.cuenta
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json

{
  "tipoOperacion": "T2T",
  "emisor": {
    "institucion": "801",
    "nombre": "Ana Ruiz Delgado"
  },
  "receptor": {
    "institucion": "802",
    "cuenta": "802180000000990030",
    "nombre": "Luis Cano Mora"
  },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "Prueba",
  "folioNumerico": 200003,
  "referenciaSeguimiento": "PRUEBAA03001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity

{
  "referenciaSeguimiento": "PRUEBAA03001",
  "errores": [
    { "codigo": "PRX-011", "campo": "emisor.cuenta", "mensaje": "La cuenta emisora es obligatoria en T2T" }
  ]
}

##  A04 — VNT con emisor.cuenta presente
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
{
  "tipoOperacion": "VNT",
  "emisor": {
    "institucion": "801",
    "cuenta": "801180000000100016",
    "nombre": "Marta Solis Vega",
    "sucursal": "0417",
    "documentoIdentidad": { "tipo": "INE", "numero": "IDMEX1734558" }
  },
  "receptor": {
    "institucion": "803",
    "cuenta": "803180000000990040",
    "nombre": "Comercial Vega SA de CV"
  },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "Prueba",
  "folioNumerico": 200004,
  "referenciaSeguimiento": "PRUEBAA04001"
}
##  Resultado esperado
HTTP 422 Unprocessable Entity
{
  "referenciaSeguimiento": "PRUEBAA04001",
  "errores": [
    { "codigo": "PRX-012", "campo": "emisor.cuenta", "mensaje": "La cuenta emisora no debe enviarse en VNT" }
  ]
}


##   A05 — VNT sin sucursal
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json

{
  "tipoOperacion": "VNT",
  "emisor": {
    "institucion": "801",
    "nombre": "Marta Solis Vega",
    "documentoIdentidad": { "tipo": "INE", "numero": "IDMEX1734558" }
  },
  "receptor": {
    "institucion": "803",
    "cuenta": "803180000000990050",
    "nombre": "Comercial Vega SA de CV"
  },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "Prueba",
  "folioNumerico": 200005,
  "referenciaSeguimiento": "PRUEBAA05001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity
{
  "referenciaSeguimiento": "PRUEBAA05001",
  "errores": [
    { "codigo": "PRX-011", "campo": "emisor.sucursal", "mensaje": "La sucursal es obligatoria en VNT" }
  ]
}


##  A06 — Cuenta receptora de 17 dígitos
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json

{
  "tipoOperacion": "T2T",
  "emisor": {
    "institucion": "801",
    "cuenta": "801180000000100016",
    "nombre": "Ana Ruiz Delgado"
  },
  "receptor": {
    "institucion": "802",
    "cuenta": "80218000000099006",
    "nombre": "Luis Cano Mora"
  },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "Prueba",
  "folioNumerico": 200006,
  "referenciaSeguimiento": "PRUEBAA06001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity

{
  "referenciaSeguimiento": "PRUEBAA06001",
  "errores": [
    { "codigo": "PRX-001", "campo": "receptor.cuenta", "mensaje": "La cuenta receptora debe tener 18 dígitos numéricos" }
  ]
}


##  A07 — Cuenta receptora con dígito verificador alterado
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json

{
  "tipoOperacion": "T2T",
  "emisor": {
    "institucion": "801",
    "cuenta": "801180000000100016",
    "nombre": "Ana Ruiz Delgado"
  },
  "receptor": {
    "institucion": "802",
    "cuenta": "802180000000990071",
    "nombre": "Luis Cano Mora"
  },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "Prueba",
  "folioNumerico": 200007,
  "referenciaSeguimiento": "PRUEBAA07001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity

{
  "referenciaSeguimiento": "PRUEBAA07001",
  "errores": [
    { "codigo": "PRX-002", "campo": "receptor.cuenta", "mensaje": "Dígito verificador de la CLABE incorrecto" }
  ]
}


##  A08 — Institución 899 inexistente
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json

{
  "tipoOperacion": "T2T",
  "emisor": {
    "institucion": "801",
    "cuenta": "801180000000100016",
    "nombre": "Ana Ruiz Delgado"
  },
  "receptor": {
    "institucion": "899",
    "cuenta": "802180000000990080",
    "nombre": "Luis Cano Mora"
  },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "Prueba",
  "folioNumerico": 200008,
  "referenciaSeguimiento": "PRUEBAA08001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity

{
  "referenciaSeguimiento": "PRUEBAA08001",
  "errores": [
    { "codigo": "PRX-003", "campo": "receptor.institucion", "mensaje": "Institución receptora no existe en el catálogo" }
  ]
}


##  A09 — Cuenta cuyos 3 primeros dígitos no coinciden con la institución
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json

{
  "tipoOperacion": "T2T",
  "emisor": {
    "institucion": "801",
    "cuenta": "801180000000100016",
    "nombre": "Ana Ruiz Delgado"
  },
  "receptor": {
    "institucion": "801",
    "cuenta": "802180000000990090",
    "nombre": "Luis Cano Mora"
  },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "Prueba",
  "folioNumerico": 200009,
  "referenciaSeguimiento": "PRUEBAA09001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity
{
  "referenciaSeguimiento": "PRUEBAA09001",
  "errores": [
    { "codigo": "PRX-030", "campo": "receptor.cuenta", "mensaje": "Los 3 primeros dígitos no coinciden con la institución" }
  ]
}


##  A10 — Importe igual a cero
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json

{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000990100", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 0, "divisa": "MXN" },
  "concepto": "Prueba",
  "folioNumerico": 200010,
  "referenciaSeguimiento": "PRUEBAA10001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity
{
  "referenciaSeguimiento": "PRUEBAA10001",
  "errores": [
    { "codigo": "PRX-004", "campo": "importe.valor", "mensaje": "El importe debe ser mayor que cero" }
  ]
}


##  A11 — Importe negativo
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json

{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000990110", "nombre": "Luis Cano Mora" },
  "importe": { "valor": -50.00, "divisa": "MXN" },
  "concepto": "Prueba",
  "folioNumerico": 200011,
  "referenciaSeguimiento": "PRUEBAA11001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity
{
  "referenciaSeguimiento": "PRUEBAA11001",
  "errores": [
    { "codigo": "PRX-004", "campo": "importe.valor", "mensaje": "El importe debe ser mayor que cero" }
  ]
}


##  A12 — Importe con tres decimales
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json

{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000990120", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 100.123, "divisa": "MXN" },
  "concepto": "Prueba",
  "folioNumerico": 200012,
  "referenciaSeguimiento": "PRUEBAA12001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity
{
  "referenciaSeguimiento": "PRUEBAA12001",
  "errores": [
    { "codigo": "PRX-005", "campo": "importe.valor", "mensaje": "Importe máximo 1,000,000.00 y máximo 2 decimales" }
  ]
}


##  A13 — Importe de 1,000,000.01
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json

{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000990130", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 1000000.01, "divisa": "MXN" },
  "concepto": "Prueba",
  "folioNumerico": 200013,
  "referenciaSeguimiento": "PRUEBAA13001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity

{
  "referenciaSeguimiento": "PRUEBAA13001",
  "errores": [
    { "codigo": "PRX-005", "campo": "importe.valor", "mensaje": "Importe máximo 1,000,000.00 y máximo 2 decimales" }
  ]
}


##  A14 — Divisa USD
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json

{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000990140", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 100.00, "divisa": "USD" },
  "concepto": "Prueba",
  "folioNumerico": 200014,
  "referenciaSeguimiento": "PRUEBAA14001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity

{
  "referenciaSeguimiento": "PRUEBAA14001",
  "errores": [
    { "codigo": "PRX-006", "campo": "importe.divisa", "mensaje": "La divisa debe ser MXN" }
  ]
}


##  A15 — Concepto vacío y folio cero en la misma petición
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000990150", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "",
  "folioNumerico": 0,
  "referenciaSeguimiento": "PRUEBAA15001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity
{
  "referenciaSeguimiento": "PRUEBAA15001",
  "errores": [
    { "codigo": "PRX-007", "campo": "concepto", "mensaje": "Concepto obligatorio entre 1 y 40 caracteres" },
    { "codigo": "PRX-008", "campo": "folioNumerico", "mensaje": "El folio numérico debe estar entre 1 y 9,999,999" }
  ]
}

## A16 — referenciaSeguimiento repetida
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000990160", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 200.00, "divisa": "MXN" },
  "concepto": "Prueba duplicado",
  "folioNumerico": 200016,
  "referenciaSeguimiento": "PRUEBAA01001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity

{
  "referenciaSeguimiento": "PRUEBAA01001",
  "errores": [
    { "codigo": "PRX-010", "campo": "referenciaSeguimiento", "mensaje": "La referencia de seguimiento ya fue registrada previamente" }
  ]
}


## A17 — T2T con emisor y receptor iguales
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "Prueba",
  "folioNumerico": 200017,
  "referenciaSeguimiento": "PRUEBAA17001"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity
{
  "referenciaSeguimiento": "PRUEBAA17001",
  "errores": [
    { "codigo": "PRX-013", "campo": "emisor.cuenta", "mensaje": "La cuenta emisora y receptora no pueden ser iguales" }
  ]
}

##  A18 — Cuenta receptora con dígitos 14–17 = 9002
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
Clave-Idempotencia: a18-{{$randomUUID}}

{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000900025", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "Prueba devolucion",
  "folioNumerico": 200018,
  "referenciaSeguimiento": "PRUEBAA18001"
}

##  Resultado esperado
Resultado esperado (POST):
HTTP 201 Created
estado = "RECIBIDO"

Resultado esperado (GET del id devuelto):
HTTP 200
estado = "DEVUELTO"
transiciones incluye { "estado": "DEVUELTO", "motivo": "PRX-020" }
Verificación: consulta GET /api/v1/operaciones/op_N y verifica estado = DEVUELTO con motivo PRX-020.

##  A19 — Cuenta receptora con 9003
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000900035", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "Prueba cuenta inexistente",
  "folioNumerico": 200019,
  "referenciaSeguimiento": "PRUEBAA19001"
}

##  Resultado esperado GET
HTTP 200
estado = "DEVUELTO"
transiciones incluye { "estado": "DEVUELTO", "motivo": "PRX-021" }



##  A20 — Operación dirigida a institución 805
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "805", "cuenta": "805180000000990200", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "Prueba institucion en mantenimiento",
  "folioNumerico": 200020,
  "referenciaSeguimiento": "PRUEBAA20001"
}

##  Resultado esperado GET
HTTP 200
estado = "DEVUELTO"
transiciones incluye { "estado": "DEVUELTO", "motivo": "PRX-022" }



##  A21 — Intento de transición inválida LIQUIDADO → DEVUELTO
Request:
PATCH http://localhost:8080/api/v1/operaciones/{{id_operacion}}/estado
Content-Type: application/json

{
  "nuevoEstado": "DEVUELTO",
  "motivo": "PRX-020"
}

##  Resultado esperado
HTTP 422 Unprocessable Entity
{
  "referenciaSeguimiento": "PRUEBAA01001",
  "errores": [
    { "codigo": "PRX-014", "campo": "estado", "mensaje": "Transición de estado no permitida" }
  ]
}


##  A22 — Consulta de un identificador inexistente
Request:
GET http://localhost:8080/api/v1/operaciones/op_999999


##  Resultado esperado
HTTP 404 Not Found
{
  "referenciaSeguimiento": null,
  "errores": [
    { "codigo": null, "campo": "id", "mensaje": "Identificador inexistente" }
  ]
}



##   A23 — Listado sin parámetros con 120 operaciones sembradas
Request:
GET http://localhost:8080/api/v1/operaciones

##  Resultado esperado
HTTP 200 OK
{
  "contenido": [ /* máx 20 elementos */ ],
  "pagina": 0,
  "tamano": 20,
  "totalElementos": 120,
  "totalPaginas": 6
}

Verificación clave:

contenido.length <= 20 (nunca la tabla completa)

totalElementos >= 120

pagina = 0, tamano = 20


##  A24 — Idempotencia: misma clave + mismo cuerpo
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
Clave-Idempotencia: a24-fija-001


{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000990240", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 500.00, "divisa": "MXN" },
  "concepto": "Prueba idempotencia",
  "folioNumerico": 200024,
  "referenciaSeguimiento": "PRUEBAA24001"
}

##  Resultado esperado 1
HTTP 201 Created
id = "op_N"

Request 2:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
Clave-Idempotencia: a24-fija-001

##  Resultado esperado 2
HTTP 200 OK
id = mismo "op_N" que el Request 1


##  A25 — Idempotencia: misma clave + cuerpo distinto
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
Clave-Idempotencia: a25-fija-001

{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000990250", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 500.00, "divisa": "MXN" },
  "concepto": "Prueba idempotencia A",
  "folioNumerico": 200025,
  "referenciaSeguimiento": "PRUEBAA25001"
}

##  Resultado esperado 1
HTTP 201 Created

Request 2:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json
Clave-Idempotencia: a25-fija-001

{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000990250", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 600.00, "divisa": "MXN" },
  "concepto": "Prueba idempotencia A",
  "folioNumerico": 200025,
  "referenciaSeguimiento": "PRUEBAA25002"
}

##  Resultado esperado 2
HTTP 409 Conflict

{
  "referenciaSeguimiento": "PRUEBAA25002",
  "errores": [
    { "codigo": "PRX-015", "campo": "Clave-Idempotencia", "mensaje": "Clave de idempotencia reutilizada con cuerpo distinto" }
  ]
}

## *A26 — Cuenta receptora con 9005
Request:
POST http://localhost:8080/api/v1/operaciones
Content-Type: application/json

{
  "tipoOperacion": "T2T",
  "emisor": { "institucion": "801", "cuenta": "801180000000100016", "nombre": "Ana Ruiz Delgado" },
  "receptor": { "institucion": "802", "cuenta": "802180000000900055", "nombre": "Luis Cano Mora" },
  "importe": { "valor": 100.00, "divisa": "MXN" },
  "concepto": "Prueba sin respuesta",
  "folioNumerico": 200026,
  "referenciaSeguimiento": "PRUEBAA26001"
}


##  Resultado esperado GET del id devuelto
HTTP 200
estado = "EN_PROCESO"
transiciones incluye { "estado": "EN_PROCESO", "motivo": "PRX-023" }
No debe existir transición posterior a LIQUIDADO/DEVUELTO


## Script para generar CLABEs válidas
function dv(p17) {
  const w = [3, 7, 1];
  let s = 0;
  for (let i = 0; i < 17; i++) s += (parseInt(p17[i]) * w[i % 3]) % 10;
  return