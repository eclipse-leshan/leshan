# Security Policy

## Reporting a Vulnerability

Currently, [GitHub security advisories](https://help.github.com/en/articles/managing-security-vulnerabilities-in-your-project) is not activated on [eclipse](https://www.eclipse.org/) project.

To report a vulnerability, your need to open a [bugzilla ticket](https://bugs.eclipse.org/bugs/enter_bug.cgi?product=Community&component=Vulnerability+Reports&keywords=security&groups=Security_Advisories).

For more details, please look at https://www.eclipse.org/security/.

## Supported Versions

Only Leshan library is concerned. The demos are not covered. 

| Version | Supported          |
| ------- | ------------------ |
| 2.x   | :heavy_check_mark: | |
| 1.x   | :heavy_check_mark: |

Note: ℹ️ **1.x** version depends on californium 2.x version where support is not clear.   
See : https://github.com/eclipse/californium/security/policy


## Versions Security State

List of  version which are not affected by known vulnerability.

| Version              |                    |
| -------------------- | ------------------ |
| 2.0.0-M9 +           | :heavy_check_mark: |
| 1.4.2 +              | :heavy_check_mark: |


This is a not exhaustive list of security issue from Leshan dependencies which could affect Leshan.

| CVE/ID                                                                                                  |  Leshan version concerned                | Source               | Affect |
| --------------------------------------------------------------------------------------------------------| ---------------------------------------- | ---------------------| ------ |
| [CVE-2022-39368](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-39368)                         | 2.0.0-M1 -> 2.0.0-M8 <br> 1.0.0 -> 1.4.1 | californium/scandium | any DTLS usage |
| [CVE-2022-2576](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-2576)                           | 2.0.0-M1 -> 2.0.0-M7 <br> 1.0.0 -> 1.4.0 | californium/scandium | DTLS_VERIFY_PEERS_ ON_RESUMPTION_THRESHOLD > 0 |
| [GHSA-fj2w-wfgv-mwq6](https://github.com/peteroupc/CBOR-Java/security/advisories/GHSA-fj2w-wfgv-mwq6)   | 2.0.0-M2 -> 2.0.0-M4                     | com.upokecenter.cbor | CBOR or SenML-CBOR decoding |
| [CVE-2020-27222](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-27222)                         | 1.1.0 -> 1.3.1                           | californium/scandium | DTLS with x509 and/or RPK  |
| [CVE-2021-34433](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-34433)                         | 2.0.0-M1 -> 2.0.0-M4 <br> 1.0.0 -> 1.3.1 | californium/scandium | DTLS with x509 and/or RPK |

Note: We strongly encourage you to switch last safe Leshan version, but for vulnerability caused by a dependency :
 - if there isn't Leshan release available OR if you want to be very conservative  
 - AND the concerned library is using [semantic versioning](https://semver.org/)
 
then you could try to just update the dependency to a safe compatible version without upgrading Leshan. 

## Runtime Security State

This is a not exhaustive list of JVM security issue which could affect common Leshan usages.

| Dependency | Affected Version | Usage | Vulnerability | More Information |
| ---------- | ---------------- | ----- | ------------- | ---------------- |
| JDK / JCE | <= 15.0.2? <br/> <= 16.0.2? <br/> < 17.0.3 <br/> < 18.0.1 | Cipher Suite based on ECDSA | ECDSA [CVE-2022-21449](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-21449) | https://github.com/eclipse/leshan/issues/1243 |
