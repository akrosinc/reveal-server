package com.revealprecision.revealserver.amdr.persistence.domain;


import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data

@Entity
@Table(name = "amdr_sample_data", schema = "amdr")
public class AmdrSampleData {

  @Id
  @Column(name = "sample_internal_id")
  private String sampleInternalId;

  @Column(name = "kelch")
  private String kelch;

  @Column(name = "pfcrt_72")
  private String pfcrt72;

  @Column(name = "pfcrt_74")
  private String pfcrt74;

  @Column(name = "pfcrt_75")
  private String pfcrt75;

  @Column(name = "pfcrt_76")
  private String pfcrt76;

  @Column(name = "pfdhfr_51")
  private String pfdhfr51;

  @Column(name = "pfdhfr_59")
  private String pfdhfr59;

  @Column(name = "pfdhfr_108")
  private String pfdhfr108;

  @Column(name = "pfdhfr_164")
  private String pfdhfr164;

  @Column(name = "pfdhps_436")
  private String pfdhps436;

  @Column(name = "pfdhps_437")
  private String pfdhps437;

  @Column(name = "pfdhps_540")
  private String pfdhps540;

  @Column(name = "pfdhps_581")
  private String pfdhps581;

  @Column(name = "pfdhps_613")
  private String pfdhps613;

  @Column(name = "pfmdr1_86")
  private String pfmdr186;

  @Column(name = "pfmdr1_184")
  private String pfmdr1184;

  @Column(name = "pfmdr1_1246")
  private String pfmdr11246;

  @Column(name = "region")
  private String region;


  @Column(name = "date_collection")
  private LocalDateTime dateCollection;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AmdrProcessingStatus status;

  private String locationIdentifier;

  private UUID importId;
}

