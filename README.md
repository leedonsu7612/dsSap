# ds-sap — SAP 실시간 조회 / 배치 추출

Java(Spring Boot)로 SAP 데이터를 **두 가지 방식**으로 가져옵니다.

| 방식 | 언제 쓰나 | 동작 |
|------|-----------|------|
| **실시간** | 화면 조회, 단건 확인, 즉시 반영이 필요할 때 | HTTP 요청 → SAP RFC/OData **동기 호출** → 바로 JSON 응답. 저장하지 않음 |
| **배치** | 야간 적재, 대량 마스터/전표 동기화 | 스케줄(또는 수동 실행) → SAP를 **페이지 단위**로 읽고 → DB + 파일에 적재 |

둘 다 같은 `SapClient`를 씁니다. 접속 방식만 `application.yml`의 `sap.client`로 고릅니다.

- `mock` : SAP 없이 샘플 데이터 (기본값, 바로 실행 가능)
- `jco` : SAP GUI와 같은 RFC (JCo). ECC / S/4 온프레미스에 가장 흔함
- `odata` : SAP Gateway / S/4HANA OData

## 실행

Java 17이 필요합니다. Maven은 프로젝트 Wrapper(`mvnw.cmd`)를 쓰므로 PATH에 `mvn`이 없어도 됩니다.

`C:\dsSap`에서:

```bat
run.bat
```

또는

```bat
mvn spring-boot:run
```

PowerShell이면 `.\run.bat` 또는 `.\mvnw.cmd spring-boot:run` 입니다.

- UI (JSON → HTML 표): http://localhost:8888/
- 실시간 자재: `GET http://localhost:8888/api/realtime/materials/10000001`
- 실시간 수주: `GET http://localhost:8888/api/realtime/sales-orders/0000000001`
- 임의 RFC: `POST http://localhost:8888/api/realtime/rfc/BAPI_MATERIAL_GET_DETAIL`  body `{"MATERIAL":"10000001"}`
- 배치 즉시 실행: `POST http://localhost:8888/api/batch/run`
- 적재 결과(MyBatis): `GET http://localhost:8888/api/mybatis/extracts?sourceTable=MARA&businessKey=10000001`
- 적재 건수(MyBatis): `GET http://localhost:8888/api/mybatis/extracts/count`
- 적재 결과(호환): `GET http://localhost:8888/api/batch/records`
- 상태: `GET http://localhost:8888/api/health`
- H2 콘솔: http://localhost:8888/h2-console  (JDBC URL `jdbc:h2:file:./data/dssap`)

## 1) 실시간

요청이 들어오는 순간에 SAP를 한 번 호출합니다. 캐시·스케줄이 없습니다.

```
클라이언트 ──GET /api/realtime/materials/{matnr}──► RealtimeSapService
                                                    │
                                         http://localhost:8888/           ▼
                                              SapClient.call()   ← 동기 RFC 또는 OData GET
                                                    │
클라이언트 ◄──────── JSON (exports + rows) ─────────┘
```

핵심 코드: `RealtimeSapService`, `RealtimeController`.

JCo일 때 실제 RFC는 아래와 같습니다.

```java
JCoDestination dest = JCoDestinationManager.getDestination("DS_SAP");
JCoFunction fn = dest.getRepository().getFunction("BAPI_MATERIAL_GET_DETAIL");
fn.getImportParameterList().setValue("MATERIAL", matnr);
fn.execute(dest);
```

이 프로젝트의 `JcoSapClient`가 위 흐름을 감쌉니다. `sapjco3.jar`는 Maven Central에 없으므로 SAP Support Portal에서 받아 `lib/`에 두고 `pom.xml` 주석을 해제하세요. 자세한 내용은 `lib/README.txt`.

OData 예 (S/4):

```
GET {base-url}/A_Product('10000001')
Authorization: Basic ...
sap-client: 100
```

## 2) 배치

매일 새벽 2시(`sap.batch.cron`)에 Spring Batch Job이 돌아갑니다. API로도 바로 실행할 수 있습니다.

```
스케줄러 / POST /api/batch/run
        │
        ▼
SapPagingItemReader  ── RFC_READ_TABLE 또는 OData $skip/$top ──► SAP (500건씩)
        │
        ▼
SapExtractProcessor  ── JSON 변환
        │
        ▼
SapExtractItemWriter ── H2 테이블 sap_extract + data/batch-out/sap-extract.jsonl
```

대량 데이터는 한 번에 가져오지 않고 `page-size`로 끊고, DB에는 `chunk-size` 단위로 커밋합니다.

운영에서는 `RFC_READ_TABLE` 대신 필요한 컬럼만 반환하는 Z Function Module을 쓰는 것을 권장합니다. `sap.batch.function-name`만 바꾸면 됩니다.

## SAP 연결 설정

`src/main/resources/application.yml`

```yaml
sap:
  client: jco          # mock | jco | odata
  jco:
    ashost: 10.0.0.10  # 앱 서버 호스트
    sysnr: "00"
    client: "100"
    user: SAP_USER
    passwd: SAP_PASSWORD
    lang: KO
  odata:
    base-url: https://s4.example.com/sap/opu/odata/sap
    username: SAP_USER
    password: SAP_PASSWORD
    sap-client: "100"
  batch:
    cron: "0 0 2 * * *"
    table-name: MARA
    page-size: 500
    fields: [MATNR, MTART, MATKL, MEINS, ERSDA]
```

비밀번호는 코드에 넣지 말고 환경 변수나 `application-local.yml`을 쓰세요.

```yaml
sap.jco.passwd: ${SAP_PASSWD}
```

## 디렉터리

```
src/main/java/com/dssap
  client/          SAP 접속 (mock / jco / odata)
  realtime/        요청 즉시 조회
  batch/           스케줄 대량 추출
  mybatis/          MyBatis 매퍼 기반 적재 조회 (동적 SQL)
  config/          application.yml 매핑
src/main/resources/mapper
  SapExtractMapper.xml
```

## 두 방식 선택 기준

- 사용자가 화면에서 **지금** 보는 값 → 실시간
- 매일 창고/자재/전표를 **통째로 우리 DB에 복사** → 배치
- 같은 데이터를 화면과 배치 모두 쓰면, 화면은 실시간 BAPI, 야간은 배치 적재 후 로컬 조회하는 식으로 나누는 경우가 많습니다.
