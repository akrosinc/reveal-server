//package com.revealprecision.revealserver.api.v1.controller;
//
//////import com.revealprecision.revealserver.messaging.message.LocationMetadataUnpackedEventWithIndividualAncestorLocation;
//////import com.revealprecision.revealserver.messaging.message.OperationalAreaVisitedCount;
////import com.revealprecision.revealserver.messaging.message.LocationMetadataEvent;
////import com.revealprecision.revealserver.messaging.message.MetaDataEvent;
////import com.revealprecision.revealserver.messaging.message.TMetadataEvent;
////    import com.revealprecision.revealserver.persistence.domain.metadata.infra.TagData;
////import com.revealprecision.revealserver.persistence.domain.metadata.infra.TagValue;
////    import com.revealprecision.revealserver.service.MetadataExpressionEvaluationService;
////    import java.time.LocalDateTime;
////import java.util.ArrayList;
////    import java.util.List;
////    import lombok.extern.slf4j.Slf4j;
////    import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.context.annotation.Profile;
////    import org.springframework.web.bind.annotation.PostMapping;
////import org.springframework.web.bind.annotation.RequestBody;
//
//import com.revealprecision.revealserver.enums.EntityStatus;
//import com.revealprecision.revealserver.integration.mail.EmailService;
//import com.revealprecision.revealserver.persistence.domain.Fields;
//import com.revealprecision.revealserver.persistence.domain.HdssCompounds;
//import com.revealprecision.revealserver.persistence.domain.Location;
//import com.revealprecision.revealserver.persistence.domain.Person;
//import com.revealprecision.revealserver.persistence.repository.HdssCompoundsRepository;
//import com.revealprecision.revealserver.persistence.repository.PersonRepository;
//import com.revealprecision.revealserver.service.LocationService;
//import com.revealprecision.revealserver.service.PersonService;
//import java.io.Serializable;
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//import java.util.Set;
//import java.util.UUID;
//import java.util.stream.Collectors;
//import lombok.Builder;
//import lombok.Data;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Profile;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@Profile("local & Testing")
//@RestController
//@RequestMapping("/test")
//@Slf4j
//@RequiredArgsConstructor
//public class TestController {
//
////  @Autowired
////  MetadataExpressionEvaluationService metadataExpressionEvaluationService;
//
//  private final HdssCompoundsRepository hdssCompoundsRepository;
//
//  private final EmailService emailService;
//
//  private final PersonService personService;
//
//  private final PersonRepository personRepository;
//
//  private final LocationService locationService;
//
//  @GetMapping(value = "/sendEmail")
//  private boolean testSend() {
//
//    try {
//      emailService.sendEmail(List.of("tbahadur@akros.com"), "From Reveal", "howdy");
//
//    }catch (Exception e){
//
//    }
//
//    return true;
//  }
//
//  @GetMapping(value = "hdss")
//  private String testhdss() {
//    List<HdssCompounds> all = hdssCompoundsRepository.findAll();
//
//    String alphas = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
//    System.out.println(getSaltString(alphas, 3));
//    String numbers = "0123456789";
//    System.out.println(getSaltString(numbers, 6));
//    System.out.println(getSaltString(numbers, 3));
//
//    int strcutureCounter = 0;
//
//    boolean male = false;
//
//    List<String> structureIds = List.of("01831f0d-d647-47fd-a669-32700969fba8",
//        "01eb5b98-4d6b-4996-95c0-faa912368c4e", "02c15ecb-8618-4e48-9636-7a8c046cbd75",
//        "03ad82ca-5375-47e3-ac2a-322cd44d91f2", "03e1c9e0-b27f-4a21-8c38-5d92769a2dd2",
//        "042b003f-173f-4e1f-bfab-9f72b9d4c144", "0520f993-745f-4735-971a-7b11c558f135",
//        "059d84f3-fdf7-46ce-a604-4df2c873202e", "08204f17-69ba-4821-82e1-9b65c6b7d3b0",
//        "085ff159-9938-40b8-a6ae-e2a5e4fa5729", "09d3f0ac-5a48-4c41-8780-1cc06e6caea2",
//        "0af3fc86-f104-4615-808c-0d41cc448fde", "0e3a0c0f-e8ff-4948-845e-9889c06b0472",
//        "0e96f1cd-96c7-45e5-8595-d1e2f86d4c3d", "0f85b2e7-6245-473f-a218-97d3d0c3affa",
//        "0fd5ce53-aab7-47d8-95f9-fbdd877c51b8", "0fe3af62-8439-4877-94bc-39257f2f4c6c",
//        "103d472b-dabf-4bfa-80ae-76412231a395", "110b8d12-4eac-41e0-801c-4d891ced5eb4",
//        "128ac5b2-3f9f-422a-8e87-ed9a904d4883", "12e4f504-066a-4a16-a4e2-40c07011c80c",
//        "16ab5604-ee9b-43a1-bb05-c29512d962ab", "1723e480-34dd-435c-be86-af125e15342e",
//        "17781d0d-0b35-4f31-8fcf-e0bb77869861", "17dae536-7711-44a7-85f4-13a37dac8beb",
//        "183ab65f-837b-4c47-8080-d9a963a13c3a", "18bb3e4c-e049-44a7-bade-f293f6b8c9fb",
//        "1ccc34b6-41ce-49fc-a784-66d82d955e0c", "1d916c06-00f3-4c97-8680-0023af737e32",
//        "1e6b7c69-0eea-4606-8859-71c7ac50cbe0", "1ee7fc70-ccc6-467d-a948-5c45570eadfd",
//        "1f2029f1-dac2-46dc-b0fd-e4415446c6cd", "1f5be72a-e718-447e-8953-0d0970d88a8a",
//        "204ce5ac-770a-4205-9727-207c93044ea2", "22c0a90a-7b9d-45e2-8d06-bfbb38b43c74",
//        "23487a86-cb12-4ec0-83db-8cc2ce41a5ee", "23ce8b82-5716-4cc6-88d7-a7a8d1fea0a4",
//        "2425d262-0e7d-49f5-8e45-1483afaeb5d5", "24b84896-11ce-4d23-9c74-7eaf80d19ffe",
//        "256b1418-4725-4c88-a929-bfcf9616c4be", "26fdfcca-bf02-4f91-a773-22790116887e",
//        "293dd5fa-c088-4b03-a773-d6492ffba541", "2a7bd45e-ec4d-48b7-9122-e0b060876d00",
//        "2c4bd47b-6108-49bd-8be4-2c604e4edda0", "2c955996-b8a7-45a5-9126-8d8b8f8f657c",
//        "2e122c13-3cae-45a9-b38e-f75598e9c90a", "3150737b-3b0a-49cc-b3fd-5aa4143baaa0",
//        "32d1ed88-c843-4a8e-87c3-7b8f94588927", "334922a6-89dc-408e-a555-f20691fb23af",
//        "3394dd50-82c9-4572-9e92-dc2dc02952a4", "33d1f01e-bbec-4626-8211-229cb91b0138",
//        "351b6015-fb12-442d-b7df-3f807df815dd", "3567db48-5e3e-4e58-9c32-838ffdf39c35",
//        "36142258-e92a-420c-bf9e-5c0c1ee2dfd4", "369e73dd-7b12-45eb-93be-c22c4f4f5a6c",
//        "36dec286-a523-4875-93b6-a15fca7521aa", "393d5758-c1af-42ff-861d-68394159cadc",
//        "3b998735-cf58-4bc2-be98-f692d74ac4c3", "3ce4b70f-81fa-4119-a4f9-ce4632619c69",
//        "3d6bfeac-cc2f-4361-b0b0-b43286bc1eb0", "3e150acf-7067-4310-83dc-415f288fba84",
//        "3f774945-ceed-4796-b5be-7a71f4c8967d", "403d9933-d28c-44d3-9e7b-d16cfe4edd5d",
//        "41da064e-5f08-4541-bad9-30bd7ff0cf40", "429eebbb-2e50-4dbe-a449-e528288e3039",
//        "43909f13-6f63-4539-9ef2-62fec5967710", "44b56f5e-35f0-4d47-a2f2-da707eb21b99",
//        "45e83cb9-8f5b-4e04-b271-e22dd1c16f30", "4631fd05-ef00-4edd-83ac-ee0602b7d9c3",
//        "46ba7505-95d9-40d8-9d9f-37ce12e2969b", "4712be66-5536-44b0-91eb-c26f2fb5d927",
//        "47269055-4613-49a5-a52b-5717eb2b7042", "48423b2e-e162-4037-91e5-32c2c4331071",
//        "484547e8-5d96-4503-a371-691db6add0ec", "48e90786-f7ce-4da0-a591-72df010ad78f",
//        "4a7fdd69-cfce-41c1-925a-07b36be4253e", "4a9cc67c-b9e6-41d5-aaf8-d9c67dac4c82",
//        "4bc5765c-759d-4515-86bd-286695b9649f", "4be419a6-bb80-43e9-8764-c8399af9d413",
//        "4d27da41-c65b-455a-a533-0205efb5894c", "4d84fd7a-3444-499c-9337-7ba18fbb7855",
//        "4e72b6c2-4d16-4543-98fc-a5fcafcc0945", "4eeb6425-ea33-4e29-a2e0-371a42adcc77",
//        "4fb98d31-2756-4532-8a74-d23922a0c84d", "509cffe8-df0b-4fb5-9cba-5c577a2e9228",
//        "50ebe556-adcb-47fe-bb6f-02202ccbf07e", "51b43b24-3f20-4edd-be6c-726b186ac851",
//        "51e77799-5136-4916-82ec-c01599555679", "54c7f7e7-5e60-439e-8a1e-ad7336d75228",
//        "56fcdd7a-b836-4983-97f5-472ea4566ed4", "5864f9d2-5d84-4e85-997c-58b46e39a4c6",
//        "589af6ae-9f39-4558-8389-40360733349c", "5a4ee5f8-a4a8-4f96-aa56-c22cd484aeca",
//        "5a54b0ac-f17c-469d-9b32-5b34143d1999", "5cad1154-8e05-4609-b6a5-16bfbc234363",
//        "5eec7879-aff4-4d4f-b627-08df534b4368", "5ef7807f-e44d-4457-b014-807b4f31427f",
//        "5fa5e121-be34-4fca-83e2-f4571e799d00", "601a649b-19a4-4797-8c20-b49f90267469",
//        "61bcf2bb-6342-44b5-ba4f-d7a25ae27f9c", "62f1f6c2-80ba-48be-ac65-c5b295397e03",
//        "640f8e6b-8931-466b-b766-24a33cc568d5", "65478de0-3e1f-4b74-a6b0-12ff60744198",
//        "65ae9e47-c53f-413f-acd7-07b37d971fed", "670678b4-8461-4ba3-8599-54d0a83f95ce",
//        "67e2d2fb-13f4-4941-9077-a7a21f86268a", "67e5e041-c06d-4f5c-a134-b88fac591027",
//        "67eb06de-6db2-40d6-aee3-0f2051da605c", "685b8ea1-5629-43b1-b3f7-5e23d753db74",
//        "6895756c-7ad8-4a17-8a1c-42745abe3cf1", "6a23e55e-3200-4662-bdc9-6da5e9a09fda",
//        "6aaa004f-974f-4e00-9cfd-fa6048a080f4", "6b816263-62b9-436c-9890-1e7a17a0d1c6",
//        "6c3a7fc0-a498-413c-b5b7-fa6d013e464b", "6e9d6e4c-d9dd-4265-8c2e-79c4a5297c7d",
//        "71c75a37-1635-47a6-80ef-5dd06c5f4f2a", "71e31683-8dd7-400b-84f1-ed6025196f1a",
//        "76660e6e-0944-4efa-95fe-12fa211e3c1f", "77985564-bd56-4978-8b6d-68c2d924c397",
//        "77b395e2-624d-4df2-9d65-7ab23e022251", "7874ec1f-0e5b-4c7f-ac8a-0e4088d58b16",
//        "7882feb0-0306-46d7-b604-c763138f009b", "792b5c9d-f762-4a43-86d0-5d125c37ec28",
//        "7a6edf32-384c-447b-a839-eaddfea34f1f", "7c42f3c8-bf12-4a80-b8ce-17c71405f537",
//        "7c4a1fe8-2498-4f52-8ddb-29b02228f798", "7c9a992b-e0c9-48ec-8cea-0c9e8e66581b",
//        "7e1ec4d8-755f-4230-a34e-a2893133c8c1", "7fd9d410-9da6-4c5f-a9d5-7bae6c5bcb89",
//        "8019915b-baa8-40e7-aab3-628836c7f7a8", "802b8ecd-9c67-42cf-84ef-ad2751758d2e",
//        "835223b4-f552-466a-9820-5fe7b273b5e1", "85903390-c98b-4065-99a9-3b6433b707de",
//        "86058d40-2764-4b56-99b4-8db8670f0e4c", "86548d6f-8928-4e1d-9007-3daceeb6c804",
//        "867d7f2a-9457-46e5-8002-a6885e705375", "87a794d5-910d-4981-a987-9426bc437d65",
//        "8827b034-66c0-4f3b-8fbb-46d250df8525", "891691fc-d4e8-41fc-907c-45f1527f7a4e",
//        "8a5f82c5-72ec-4b92-b75f-b7c77d09baa4", "8b06bec5-e7da-497c-b734-5bcac37adad2",
//        "8c94ed75-3c96-41ae-a3e0-c328b0877e6a", "8dc40fc1-82c2-402d-ae3a-fd02a4f2dfa1",
//        "8e9edc04-9a48-4479-8da3-6c9eba6a7477", "8e9f6d38-dbac-4318-b47f-4b64950fa0e2",
//        "901ea6b8-7f18-4a95-8641-17d569e8023c", "915ab5f3-7d48-4b29-b313-c2006da161ae",
//        "933ec705-b54c-4041-b88d-6aee40b98aaa", "96205701-e4b0-4ea9-8118-a1240a94afbe",
//        "96c103d0-6c45-4d48-b486-a1c4567da7ae", "9a7abd43-557c-4965-b867-333e58ae1f18",
//        "9ab16b74-e9d9-4225-a223-23d9b012c4fe", "9c12c6e5-89bb-4ff1-9f19-4834f3e004fd",
//        "9e9d83f6-57bf-4353-8340-29f69a298945", "a20e4517-a604-4f3e-b063-f0ef1df34086",
//        "a24bc82f-e28d-4b8e-9724-65b690d351f8", "a28264a1-f9d5-4b24-a068-15a528d86b77",
//        "a59e826b-790d-4a08-ac76-15cc30e1aae5", "a88c4c3c-3675-41e4-b817-7e54c513e3e1",
//        "a93a3a9e-9c97-42eb-a72d-a1a56a4d2f8f", "aaa2c645-8540-4b89-b419-fb1bd276715d",
//        "aabe4ff3-e3fa-421e-9b67-0d8ba571a623", "adb52344-4801-437f-aaf1-1c4bddf113bb",
//        "ae2a9435-3579-4f98-96f6-ca6bf53f2c08", "af3f63b2-31e3-45ab-a811-cc41ef9c2551",
//        "b091804e-8f04-47fd-9162-ba379d348b99", "b1b3c0d4-d471-4a53-9c41-c54836033baf",
//        "b2cf2d2f-7f8d-450f-a153-52410525bedf", "b2fae593-81ce-41b3-8ac2-e1443c276452",
//        "b463c273-c38a-4990-aaec-4222bd3ed0b7", "b4fb71c6-3d58-4a89-b66c-d31f233ec309",
//        "b4febb54-2320-4b0a-89c4-ee0a0010b76a", "b62e699a-c989-4b64-9ae0-c8eb8951cc44",
//        "b7e1b6a0-477d-4a81-a679-9447dc395132", "b815cc5d-04ff-47a2-9771-d37b64366e2f",
//        "b910fe2a-fbd6-4a27-bf15-824b50361f9a", "ba39020d-6580-4c89-be77-d9a4538d3c40",
//        "bb9e4f7a-3361-4625-b23c-1420be4d6774", "bde17a0d-20ef-4667-928f-0c1ef41d55b8",
//        "be9f77ce-36c4-47f2-957c-34552a53352a", "c052cc73-1bd3-4f8c-87bd-729a5ec53517",
//        "c0ab72f6-30a5-4984-b237-858061e55374", "c13c06c1-1c3b-48d6-b481-b538a09150d9",
//        "c50df4ab-43b6-4abc-8cd3-78de475210f1", "c5da5d97-bb5a-40f1-9eb7-1b673d660d55",
//        "c7903ca6-a727-4017-a3c7-455fa1f0b5e8", "c7dd3d01-fbbe-4008-a898-ad7e6d68c4c0",
//        "c9f35d1e-6cf2-4135-a511-86a7316bb5a3", "ca022584-9f9f-4353-9d21-3d395f752e10",
//        "ca160052-6172-458e-a1d6-91bce009f17a", "cb79785b-55a7-4e74-b02a-21506c9d1ab0",
//        "cbc4fcb7-0296-46bb-aabf-29974f181048", "cbf8cb8d-334a-4c9b-94be-283470114341",
//        "cc0338a9-0d52-473c-954a-78b2673224e0", "cc172669-5dff-4ba1-b664-1d290f095b19",
//        "cc24d66d-ac7c-43e3-9446-38ba2f65635d", "ce683b92-038f-4706-b728-454a2978b0f0",
//        "cf018db9-b789-4ec2-9b18-094e9da41695", "cf0cb5ea-2f8c-432e-b9ae-eec7861d82ba",
//        "cfd475a0-d37d-41f5-9077-3e221bf7cd33", "d116abf0-9821-4a16-b3e8-85ea2aa4950a",
//        "d13f3c14-1dbb-42e8-bd2d-706116b8511d", "d3e38e36-4bfa-48dc-97c8-b2b1b0627d1d",
//        "d440d062-4aa7-4987-b695-4596bb82bf47", "d7fcbf44-465c-4d07-8635-73bc1b4811d9",
//        "d92e37b2-c0f5-4574-b0d6-bc0e04403162", "d9900bd4-81ac-4944-8905-098c1c6609ba",
//        "d9b56bdb-e86c-41b9-8219-52d0778ed2dd", "dd8f58c1-27af-4295-813c-350b4ce47a00",
//        "de0f4f97-f8fc-4933-badd-96b7e17c471d", "df3df31a-063f-4bef-a557-a5f964f0ac63",
//        "e00c34f8-6544-44ea-a5fc-3b52984f9a21", "e02986bf-97c7-4c2b-a899-e56e83c02d75",
//        "e0809034-71a1-4dc5-b6d5-a36baa7d3159", "e092b94c-3a09-46e6-b32d-d888416e1140",
//        "e0cf6103-f0cb-4087-9b6d-6423d5f30f94", "e15ca883-3e48-47cb-810d-333ad3962bcd",
//        "e25c91be-d1b1-4f82-bc20-43fcfd65b56a", "e3e2b06c-7400-46ed-944f-e82ea7d6b384",
//        "e7a9d24b-a9f7-422c-a31a-c8bc7ed16649", "e892349d-d391-41b3-a7b9-6981d572dc20",
//        "ec7af293-f718-48e0-9cdf-28d2cc7764be", "ecd0a8fb-3182-4052-bada-b4b035d39b71",
//        "ee47d304-aa61-42d3-9e72-db8e9f97bbc0", "ef0485d7-ed2d-4074-b407-1d2f80d3a9c6",
//        "f0ff314c-1fd3-45c4-b9a2-ea385178008a", "f1771511-8c3d-4118-a16f-7d269d8184e1",
//        "f558f3f7-6458-4342-8a41-2520e9073e4e", "f75d6da5-ad27-4c08-92fa-b843a02e793b",
//        "fb34ff3a-d846-4a98-8e83-73896f4032ae", "fc318365-8c24-4145-ac84-748d4470410c",
//        "fc516da0-0e47-4927-ab3c-4b8041d38504", "fcd1e940-02fe-4e7f-a305-9cb4a088f2bd",
//        "fdef3247-1348-4124-a0b7-0c04f9d9a57f", "ffb05bf1-5c94-4330-bc75-587c491a7967");
//
//    List<HdssCompounds> hdssCompoundList = new ArrayList<>();
//    Set<IndividualPersonStructure> individualPersonStructures = new HashSet<>();
//
//    for (int i = 0; i < 100 && strcutureCounter < structureIds.size(); i++) {
//      String alphaPrefix = getSaltString(alphas, 3);
//
//      String compoundSuffix = getSaltString(numbers, 6);
//
//      String compoundId = alphaPrefix.concat(compoundSuffix);
//
//      int randNumber = getRandNumber(10);
//      for (int j = 0; j < randNumber && strcutureCounter < structureIds.size(); j++) {
//
//        String householdSuffix = getSaltString(numbers, 3);
//
//        String householdId = compoundId.concat(householdSuffix);
//        String structure = structureIds.get(strcutureCounter++);
//
//
//        UUID structureId = UUID.fromString(structure);
//
//        int randNumber1 = getRandNumber(15);
//        for (int k = 0; k < randNumber1; k++) {
//          if (strcutureCounter == 200) {
//            break;
//          }
//          String individualSuffix = getSaltString(numbers, 3);
//          String individualId = householdId.concat(individualSuffix);
//
//          String gender = male ? "Male" : "Female";
//          LocalDate randomDob = getRandomDob();
//
//          hdssCompoundList.add(HdssCompounds.builder()
//              .compoundId(compoundId)
//              .householdId(householdId)
//              .individualId(individualId)
//              .structureId(structureId)
//              .fields(Fields.builder()
//                  .dob(randomDob)
//                  .gender(gender)
//                  .build())
//              .build());
//
//          male = !male;
//
//        }
//
//      }
//    }
//    List<HdssCompounds> hdssCompounds = hdssCompoundsRepository.saveAll(hdssCompoundList);
//
////    Map<UUID, IndividualPersonStructure> people = hdssCompounds.stream()
////        .collect(Collectors.toMap(HdssCompounds::getId, hdssCompound ->
////            IndividualPersonStructure.builder()
////                .individualPerson(IndividualPerson.builder()
////                    .identifier(hdssCompound.getId())
////                    .individualId(hdssCompound.getIndividualId())
////                    .dob(Date.from(
////                        hdssCompound.getFields().getDob().atStartOfDay(ZoneId.systemDefault())
////                            .toInstant()))
////                    .gender(hdssCompound.getFields().getGender())
////                    .build())
////                .structureId(hdssCompound.getStructureId()).build(),(a,b)->b)
////        );
//
//    Set<UUID> uniqueStructures = hdssCompounds.stream().map(HdssCompounds::getStructureId)
//        .collect(Collectors.toSet());
//
//    Set<Location> locationsWithoutGeoJsonByIdentifierIn = locationService.findLocationsWithoutGeoJsonByIdentifierIn(
//        uniqueStructures);
//
//    Map<UUID, Location> peoplesStructures = locationsWithoutGeoJsonByIdentifierIn.stream()
//        .collect(Collectors.toMap(Location::getIdentifier, a -> a, (a, b) -> b));
//
//    List<Person> peopleToSave = people.values().stream()
//        .filter(
//            individualPersonStructure -> peoplesStructures.containsKey(
//                individualPersonStructure.getStructureId()))
//        .map(individualPersonStructure -> {
//          Person person = Person.builder()
//              .locations(
//              Set.of(peoplesStructures.get(individualPersonStructure.getStructureId())))
//              .birthDate(individualPersonStructure.getIndividualPerson().getDob())
//              .nameText(individualPersonStructure.getIndividualPerson().getIndividualId())
//              .identifier(individualPersonStructure.getIndividualPerson().getIdentifier())
//              .gender(individualPersonStructure.getIndividualPerson().getGender())
//              .nameUse(individualPersonStructure.getIndividualPerson().getIndividualId())
//              .nameSuffix(individualPersonStructure.getIndividualPerson().getIndividualId())
//              .nameFamily(individualPersonStructure.getIndividualPerson().getIndividualId())
//              .nameGiven(individualPersonStructure.getIndividualPerson().getIndividualId())
//              .namePrefix(individualPersonStructure.getIndividualPerson().getIndividualId())
//              .build();
//          person.setEntityStatus(EntityStatus.ACTIVE);
//          return person;
//        }).collect(Collectors.toList());
//
//      personRepository.saveAll(peopleToSave);
//
////    individualPersonStructures.add(IndividualPersonStructure.builder()
////        .individualPerson(IndividualPerson.builder()
////            .individualId(individualId)
////            .dob(Date.from(randomDob.atStartOfDay(ZoneId.systemDefault()).toInstant()))
////            .gender(gender)
////            .build())
////        .structureId(structureId)
////        .build());
//
//
//
//    return String.valueOf(hdssCompounds.size());
//  }
//
//
//  @Data
//  @Getter
//  @Builder
//  public static class IndividualPersonStructure {
//    private IndividualPerson individualPerson;
//    private UUID structureId;
//  }
//
//  @Data
//  @Builder
//  public static class IndividualPerson{
//    private String individualId;
//    private UUID identifier;
//    private String gender;
//    private Date dob;
//
//  }
//
////  @GetMapping(value = "compounds")
////  public Setting<KV> getCompounds() {
////
////    List<HdssCompounds> all = hdssCompoundsRepository.findAll();
////    Map<String, Set<KV>> collect = all.stream()
////        .collect(Collectors.groupingBy(HdssCompounds::getCompoundId,
////            Collectors.mapping(hdssCompounds -> KV.builder()
////                .name(hdssCompounds.getHouseholdId())
////                .code(hdssCompounds.getStructureId().toString())
////                .build(), Collectors.toSet())));
////
////    Setting<KV> setting = new Setting<>();
////    setting.setKey("hdss_compounds");
////    setting.setValues(List.of(collect));
////    return setting;
////  }
////
////  @GetMapping(value = "individuals")
////  public Setting<K> individuals() {
////
////    List<HdssHouseholdIndividualProjection> all = hdssCompoundsRepository.getAll();
////    Map<String, Set<K>> collect = all.stream()
////        .collect(
////            Collectors.groupingBy(
////                hdssHouseholdIndividualProjection -> hdssHouseholdIndividualProjection.getHouseholdId(),
////                Collectors.mapping(hdssHouseholdIndividualProjection -> K.builder()
////                    .name(hdssHouseholdIndividualProjection.getIndividualId())
////                    .build(), Collectors.toSet())));
////
////    Setting<K> setting = new Setting<>();
////    setting.setKey("hdss_individuals");
////    setting.setValues(List.of(collect));
////    return setting;
////  }
//
//  @Data
//  @NoArgsConstructor
//  public static class Setting<T> implements Serializable {
//
//    public String key;
//    public String value;
//    public String label = "";
//    public String description = "";
//    public String settingsIdentifier = "global_configs";
//    public String type = "global_configs";
//    public List<Map<String, Set<T>>> values;
//  }
//
//  @Data
//  @Builder
//  public static class KV implements Serializable {
//
//    public String code;
//    public String name;
//  }
//
//  @Data
//  @Builder
//  public static class K implements Serializable {
//
//    public String name;
//  }
//
//  public static void main(String[] args) {
//    for (int i = 0; i < 3; i++) {
//      int numMax = 2022;
//
//      int random = getRandNumber(70);
//
//      System.out.println(numMax - random);
//
//      LocalDate localDate = LocalDate.ofYearDay(numMax - random, getRandNumber(365));
//      System.out.println(localDate);
//    }
//  }
//
//  private static String getSaltString(String SALTCHARS, int len) {
//
//    StringBuilder salt = new StringBuilder();
//    Random rnd = new Random();
//    while (salt.length() < len) { // length of the random string.
//      int index = (int) (rnd.nextFloat() * SALTCHARS.length());
//      salt.append(SALTCHARS.charAt(index));
//    }
//    return salt.toString();
//  }
//
//  private static int getRandNumber(int max) {
//    Random random = new Random();
//    return (int) (random.nextFloat() * max);
//  }
//
//  private static LocalDate getRandomDob() {
//    int numMax = 2022;
//
//    int random = getRandNumber(70);
//
//    int dayOfWeek = getRandNumber(365);
//    return LocalDate.ofYearDay(numMax - random, dayOfWeek == 0 ? dayOfWeek + 2 : dayOfWeek);
//  }
//
////  @PostMapping(value = "/expressionEvaluator")
////  public Object expressionEvaluator(@RequestBody String expression) throws NoSuchMethodException {
////
////    FormDataEntityTagEvent entityTagEvent = new FormDataEntityTagEvent();
////
////    List<FormDataEntityTagValueEvent> formDataEntityTagValueEvents = new ArrayList<>();
////
////    FormDataEntityTagValueEvent formDataEntityTagValueEvent1 = new FormDataEntityTagValueEvent();
////    EntityTagEvent entityTagEvent1 = new EntityTagEvent();
////    entityTagEvent1.setTag("total-females");
////    entityTagEvent1.setValueType("integer");
////    formDataEntityTagValueEvent1.setValue("1");
////    formDataEntityTagValueEvent1.setEntityTagEvent(entityTagEvent1);
////    formDataEntityTagValueEvents.add(formDataEntityTagValueEvent1);
////
////    FormDataEntityTagValueEvent formDataEntityTagValueEvent2 = new FormDataEntityTagValueEvent();
////    EntityTagEvent entityTagEvent2 = new EntityTagEvent();
////    entityTagEvent2.setTag("total-males");
////    entityTagEvent2.setValueType("integer");
////    formDataEntityTagValueEvent2.setValue("1");
////    formDataEntityTagValueEvent2.setEntityTagEvent(entityTagEvent2);
////    formDataEntityTagValueEvents.add(formDataEntityTagValueEvent2);
////
////    entityTagEvent.setFormDataEntityTagValueEvents(formDataEntityTagValueEvents);
////
////    return metadataExpressionEvaluationService.evaluateExpression(expression,FormDataEntityTagEvent.class ,entityTagEvent, Integer.class, null);
////
////
////  }
//
//
//}
//
//
//
//
//
//
//
//
