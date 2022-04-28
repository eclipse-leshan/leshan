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

<table>
    <thead>
        <tr>
            <th width=200>Version</th>
            <th></th>
            <th>CVE/ID</th>
            <th>cause</th>
            <th>affect</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td> 2.0.0-M7 + </td>
            <td> :heavy_check_mark: </td>
            <td />
            <td />
            <td />
        </tr>
        <tr>
            <td> 2.0.0-M5 -> 2.0.0-M6 </td>
            <td> :x: </td>
            <td> <a href="https://github.com/peteroupc/CBOR-Java/security/advisories/GHSA-fj2w-wfgv-mwq6">GHSA-fj2w-wfgv-mwq6</a> </td>
            <td> dependency (com.upokecenter.cbor) </td>
            <td> CBOR or SenML-CBOR decoding </td>
        </tr>
        <tr>
            <td rowspan=2> 2.0.0-M2 -> 2.0.0-M4</td>
            <td rowspan=2> :x: </td>
                <td> <a href="https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-34433">CVE-2021-34433</a> </td>
                <td> dependency (californium/scandium) </td>
                <td> DTLS with x509 and/or RPK  </td>
            <tr>
                <td> <a href="https://github.com/peteroupc/CBOR-Java/security/advisories/GHSA-fj2w-wfgv-mwq6">GHSA-fj2w-wfgv-mwq6</a> </td>
                <td> dependency (com.upokecenter.cbor) </td>
                <td> CBOR or SenML-CBOR decoding </td>
            </tr>
        </tr>
        <tr>
            <td> 2.0.0-M1 </td>
            <td> :x: </td>
            <td> <a href="https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-34433">CVE-2021-34433</a> </td>
            <td> dependency (californium/scandium) </td>
            <td> DTLS with x509 and/or RPK  </td>
        </tr>
         <tr>
            <td> 1.3.2 +  </td>
            <td> :heavy_check_mark: </td>
            <td />
            <td />
            <td />
        </tr>
        <tr>
            <td> 1.1.0 -> 1.3.1 </td>
            <td> :x: </td>
            <td> <a href="https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-27222">CVE-2020-27222</a>
                 <a href="https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-34433">CVE-2021-34433</a>
            </td>
            <td> dependency (californium/scandium) </td>
            <td> DTLS with x509 and/or RPK  </td>
        </tr>
        <tr>
            <td> 1.0.0 -> 1.0.2 </td>
            <td> :x: </td>
            <td> <a href="https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-34433">CVE-2021-34433</a> </td>
            <td> dependency (californium/scandium) </td>
            <td> DTLS with x509 and/or RPK  </td>
        </tr>
    </tbody>
</table>

Note: We strongly encourage you to switch last safe Leshan version, but for vulnerability caused by a dependency :
 - if there isn't Leshan release available OR if you want to be very conservative  
 - AND the concerned library is using [semantic versioning](https://semver.org/)
 
then you could try to just update the dependency to a safe compatible version without upgrading Leshan. 

## Runtime Security State

This is a not exhaustive list of JVM security issue which could affect common Leshan usages.

| Dependency | Affected Version | Usage | Vulnerability | More Information |
| ---------- | ---------------- | ----- | ------------- | ---------------- |
| JDK / JCE | <= 15.0.2? <br/> <= 16.0.2? <br/> < 17.0.3 <br/> < 18.0.1 | Cipher Suite based on ECDSA | ECDSA [CVE-2022-21449](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-21449) | https://github.com/eclipse/leshan/issues/1243 |
