# Security Policy


This Eclipse Foundation Project adheres to the [Eclipse Foundation Vulnerability Reporting Policy](https://www.eclipse.org/security/policy/).

## How To Report a Vulnerability

If you think you have found a vulnerability in this repository, please report it to us through coordinated disclosure.

**Please do not report security vulnerabilities through public issues, discussions, or change requests.**

Instead, report it using one of the following ways:

* (**Preferred way ⭐**) Report a [vulnerability](https://github.com/eclipse-leshan/leshan/security/advisories/new) directly via private vulnerability reporting on GitHub
* Create a [confidential issue](https://gitlab.eclipse.org/security/vulnerability-reports/-/work_items/new?issue[confidential]=true) in the Eclipse Foundation Vulnerability Reporting Tracker

You can find more information about reporting and disclosure at the [Eclipse Foundation Security page](https://www.eclipse.org/security/).

Please include as much of the information listed below as you can to help us better understand and resolve the issue:

* The type of issue (e.g., buffer overflow, SQL injection, or cross-site scripting)
* Affected version(s)
* Impact of the issue, including how an attacker might exploit the issue
* Step-by-step instructions to reproduce the issue
* The location of the affected source code (tag/branch/commit or direct URL)
* Full paths of source file(s) related to the manifestation of the issue
* Configuration required to reproduce the issue
* Log files that are related to this issue (if possible)
* Proof-of-concept or exploit code (if possible)

This information will help us triage your report more quickly.

## Supported Versions

Only Leshan library is concerned. The demos are not covered.

| Version | Supported          |
| ------- | ------------------ |
| 2.x     | :heavy_check_mark: |
| 1.x     | :heavy_check_mark: |

Note: ℹ️ **1.x** version depends on californium 2.x version where support is not clear.  
See : https://github.com/eclipse/californium/security/policy

## About Leshan Demo

As said previously **Leshan demos are not covered by Security Policy**.

It is strongly discouraged to use Leshan demos v1.x on public server because they are using no longer maintained javascript/java library like :

- **bootstrap.js** (pkg:javascript/bootstrap@3.4.1) : Bootstrap before 4.0.0 is end-of-life and no longer maintained.
- **jquery-2.2.4.js** (pkg:javascript/jquery@2.2.4) : CVE-2015-9251, CVE-2019-11358, CVE-2020-11022, CVE-2020-11023, jQuery 1.x and 2.x are End-of-Life and no longer receiving security updates
- [**Jetty 9.x**](https://github.com/jetty/jetty.project/issues/7958) : Jetty 9.4 reach End-of-Life and no longer receiving security updates at January 2025.
- [**logback 1.2.x**](https://logback.qos.ch/download.html) : The 1.2.x series has been deprecated for several years and is no longer maintained. As such, use of the 1.2.x series is discouraged. (CVE-2024-12798, CVE-2024-12801)


Concerning Leshan demos v2.x, some minimal efforts are made to update dependencies when vulnerabilities are detected but keep in mind that demos are not production ready tools.

## Versions Security State

List of version which are not affected by known vulnerability.

| Version     |                    |
| ----------- | ------------------ |
| 2.0.0-M13 + | :heavy_check_mark: |
| 1.5.0 +     | :heavy_check_mark: |

This is an exhaustive list of known security issue affecting leshan library :

| CVE/ID                                                                                                                                                                                       | Leshan version concerned                  | artifacts         | Affect                                                                                           |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------- | ----------------- | ------------------------------------------------------------------------------------------------ |
| [CVE-2023-41034](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-41034) <br> [GHSA-wc9j-gc65-3cm7](https://github.com/eclipse-leshan/leshan/security/advisories/GHSA-wc9j-gc65-3cm7) | 2.0.0-M1 -> 2.0.0-M12 <br> 1.0.0 -> 1.4.2 | leshan-lwm2m-core | if you parse untrusted DDF files <br> (e.g. if they let external users provide their own model), |

This is a not exhaustive list of security issue from Leshan dependencies which could affect Leshan :

| CVE/ID                                                                                                | Leshan version concerned                 | Source               | Affect                                         |
| ----------------------------------------------------------------------------------------------------- | ---------------------------------------- | -------------------- | ---------------------------------------------- |
| [CVE-2022-39368](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-39368)                       | 2.0.0-M1 -> 2.0.0-M8 <br> 1.0.0 -> 1.4.1 | californium/scandium | any DTLS usage                                 |
| [CVE-2022-2576](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-2576)                         | 2.0.0-M1 -> 2.0.0-M7 <br> 1.0.0 -> 1.4.0 | californium/scandium | DTLS*VERIFY_PEERS* ON_RESUMPTION_THRESHOLD > 0 |
| [GHSA-fj2w-wfgv-mwq6](https://github.com/peteroupc/CBOR-Java/security/advisories/GHSA-fj2w-wfgv-mwq6) | 2.0.0-M2 -> 2.0.0-M4                     | com.upokecenter.cbor | CBOR or SenML-CBOR decoding                    |
| [CVE-2020-27222](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-27222)                       | 1.1.0 -> 1.3.1                           | californium/scandium | DTLS with x509 and/or RPK                      |
| [CVE-2021-34433](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-34433)                       | 2.0.0-M1 -> 2.0.0-M4 <br> 1.0.0 -> 1.3.1 | californium/scandium | DTLS with x509 and/or RPK                      |

Note: We strongly encourage you to switch last safe Leshan version, but for vulnerability caused by a dependency :

- if there isn't Leshan release available OR if you want to be very conservative
- AND the concerned library is using [semantic versioning](https://semver.org/)

then you could try to just update the dependency to a safe compatible version without upgrading Leshan.

## Runtime Security State

This is a not exhaustive list of JVM security issue which could affect common Leshan usages.

| Dependency | Affected Version                                          | Usage                       | Vulnerability                                                                         | More Information                              |
| ---------- | --------------------------------------------------------- | --------------------------- | ------------------------------------------------------------------------------------- | --------------------------------------------- |
| JDK / JCE  | <= 15.0.2? <br/> <= 16.0.2? <br/> < 17.0.3 <br/> < 18.0.1 | Cipher Suite based on ECDSA | ECDSA [CVE-2022-21449](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-21449) | https://github.com/eclipse/leshan/issues/1243 |
