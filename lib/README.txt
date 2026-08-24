SAP JCo (Java Connector) 설치

1. SAP Support Portal 에서 Java Connector 3.x 를 받습니다.
   - sapjco3.jar
   - Windows: sapjco3.dll
   - Linux: libsapjco3.so

2. 이 폴더에 sapjco3.jar 를 복사합니다.

3. 네이티브 라이브러리는 PATH(Windows) 또는 java.library.path 에 둡니다.

4. pom.xml 의 sapjco3 system 의존성 주석을 해제합니다.

5. application.yml:
   sap:
     client: jco
