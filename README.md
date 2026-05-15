# P2P Chat

## Modules

- `peer-node`: ung dung Swing cua peer, gui/nhan chat truc tiep qua TCP.
- `bootstrap-server`: tracker chay rieng, quan ly JOIN/LEAVE/LIST peer online va luu SQLite.

Hai module khong import class cua nhau. Bootstrap co DTO rieng trong package `dungcony.ds.models`.
Bootstrap SQLite mac dinh nam tai `bootstrap-server/src/main/resources/database/bootstrap-server.db`, cau hinh trong `bootstrap-server/src/main/resources/config.properties`.

## Run

```bat
run.bat
```

Chay bootstrap server:

```bat
run-bootstrap.bat
```

Hoac chay bang Maven:

```bat
mvn -pl peer-node exec:java -Dexec.mainClass="dungcony.ds.App"
mvn -pl bootstrap-server exec:java -Dexec.mainClass="dungcony.ds.App" -Dexec.args="9000"
```

## Build

```bat
mvn test
```
