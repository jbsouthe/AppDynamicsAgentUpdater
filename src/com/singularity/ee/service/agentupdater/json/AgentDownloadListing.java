package com.singularity.ee.service.agentupdater.json;

import java.util.List;

/*
curl "https://download.appdynamics.com/download/downloadfile/?version=22.2&apm=jvm%2Cjava-jdk8"
{"count":3,"next":null,"previous":null,
"results":[
    {"id":19335,
    "filename":"AppServerAgent-ibm-22.2.0.33545.zip",
    "s3_path":"download-file/ibm-jvm/22.2.0.33545/AppServerAgent-ibm-22.2.0.33545.zip",
    "title":"Java Agent Legacy - IBM JVM",
    "description":"Agent to monitor Java applications running on legacy JRE versions (1.6 and 1.7) on the IBM J9 JVM",
    "download_path":"https://download.appdynamics.com/download/prox/download-file/ibm-jvm/22.2.0.33545/AppServerAgent-ibm-22.2.0.33545.zip",
    "filetype":"ibm-jvm",
    "version":"22.2.0.33545",
    "bit":null,
    "os":"",
    "extension":"zip",
    "sha256_checksum":"16ef47ee561dcf16d68f0ada3892e7d8fd87a29edbe8a4cabc5127f844fbfd39",
    "md5_checksum":"f2a2c5264d6ba9189733997a1d73f02e",
    "file_size":"21.6",
    "is_visible":true,
    "is_beta":false,
    "is_fcs":false,
    "creation_time":"2022-02-24T12:50:13.709357Z",
    "post_download_information":"",
    "installation_link":"",
    "required_controller_version":null,
    "major_version":22,
    "minor_version":2,
    "hotfix_version":0,
    "build_number":33545,
    "release_notes_url":""},
    {"id":19336,"filename":"AppServerAgent-22.2.0.33545.zip","s3_path":"download-file/sun-jvm/22.2.0.33545/AppServerAgent-22.2.0.33545.zip","title":"Java Agent Legacy - Sun and JRockit","description":"Agent to monitor Java applications running on legacy JRE versions (1.6 and 1.7) on HotSpot or JRockit based JVMs","download_path":"https://download.appdynamics.com/download/prox/download-file/sun-jvm/22.2.0.33545/AppServerAgent-22.2.0.33545.zip","filetype":"sun-jvm","version":"22.2.0.33545","bit":null,"os":"","extension":"zip","sha256_checksum":"ba9683facc9f4bedf25dbd57c9ba7be36d49c18f7c14ab92cc4ad3538534fb76","md5_checksum":"8d3e947852d6fd0ab6d8426d4db4178a","file_size":"21.6","is_visible":true,"is_beta":false,"is_fcs":false,"creation_time":"2022-02-24T12:50:13.524530Z","post_download_information":"","installation_link":"","required_controller_version":null,"major_version":22,"minor_version":2,"hotfix_version":0,"build_number":33545,"release_notes_url":""},
    {"id":19334,"filename":"AppServerAgent-1.8-22.2.0.33545.zip","s3_path":"download-file/java-jdk8/22.2.0.33545/AppServerAgent-1.8-22.2.0.33545.zip","title":"Java Agent JDK8+","description":"Agent to monitor Java applications (All Vendors) running on JRE version 1.8 and above.","download_path":"https://download.appdynamics.com/download/prox/download-file/java-jdk8/22.2.0.33545/AppServerAgent-1.8-22.2.0.33545.zip","filetype":"java-jdk8","version":"22.2.0.33545","bit":null,"os":"","extension":"zip","sha256_checksum":"415793e0c63b6db01f913dd588a3377f7a589352b94e1010f7665d4302ffc175","md5_checksum":"197dc719307e937708e84bc2b7535723","file_size":"44.4","is_visible":true,"is_beta":false,"is_fcs":false,"creation_time":"2022-02-24T12:50:12.613119Z","post_download_information":"","installation_link":"","required_controller_version":null,"major_version":22,"minor_version":2,"hotfix_version":0,"build_number":33545,"release_notes_url":""}]}%
 */
public class AgentDownloadListing {
    public int count;
    public List<DownloadDetails> results;

    public AgentDownloadListing() {}

    public DownloadDetails getBestAgent() {
        if( results == null || results.size() == 0 ) return null;
        if( results.size() == 1 ) return results.get(0);
        for( DownloadDetails downloadDetail : results )
            if( downloadDetail.filetype.equals("java-jdk8")) return downloadDetail;
        return null;
    }

}
