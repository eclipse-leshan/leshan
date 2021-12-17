# Security Policy

## Reporting a Vulnerability

Currently, [GitHub security advisories](https://help.github.com/en/articles/managing-security-vulnerabilities-in-your-project) is not activated on [eclipse](https://www.eclipse.org/) project.

To report a vulnerability, your need to open a [bugzilla ticket](https://bugs.eclipse.org/bugs/enter_bug.cgi?product=Community&component=Vulnerability+Reports&keywords=security&groups=Security_Advisories).

For more details, please look at https://www.eclipse.org/security/.

## Supported Versions

Only Leshan library is concerned. The demos are not covered. 

| Version | Supported          |
| ------- | ------------------ |
| 2.0.0 (master)   | :heavy_check_mark: | |
| 1.x   | :heavy_check_mark: |

Note: ℹ️ **1.x** version depends on californium 2.x version where support is not clear.   
See : https://github.com/eclipse/californium/security/policy


## Versions Security State

| Version | Safe          | CVE | cause | | 
| ------- | ------------- |-----|------|-|
| 2.0.0-M5 + | :heavy_check_mark: | | | | 
| 2.0.0-M1 -> 2.0.0-M4   | :x: | [CVE-2021-34433](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-34433)| dependency (californium/scandium) |affecting DTLS with x509 and/or RPK |
| 1.3.2 +  | :heavy_check_mark: |
| 1.1.0 -> 1.3.1 | :x: | [CVE-2020-27222](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-27222) [CVE-2021-34433](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-34433)| dependency (californium/scandium) |affecting DTLS with x509 and/or RPK |
| 1.0.0 -> 1.0.2  | :x: | [CVE-2021-34433](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-34433)| dependency (californium/scandium) |affecting DTLS with x509 and/or RPK |

Note: We strongly encourage you to switch last safe version, but for vulnerability caused by a dependency :
 - if you want to be very conservative
 - and the concerned library is uing [semantic versioning](https://semver.org/)
 
then you could try to just update the dependency to a safe compatible version without upgrading Leshan. 