package com.volunteerintegration.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PublicDataParserTest {

    @Test
    void 같은_활동의_실적_행을_하나로_묶는다(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("v.csv");
        Files.writeString(csv, "﻿봉사ID,시설ID,매칭,봉사활동명,수요기관명,수요기관지역,소속지역,봉사일자,"
                + "시작시,시작분,종료시,종료분,봉사시간,활동내용,실적인정일,실적인정시간,실적인정분,작업상태\n"
                + "VOL-1,,,\"하천 정화, 플로깅\",A센터,밀양시,경상남도,20211231,14,00,17,00,3,정화,20220101,,,C\n"
                + "VOL-2,FAC-1,이름일치,\"하천 정화, 플로깅\",A센터,밀양시,경상남도,20211231,14,00,17,00,3,정화,20220101,,,C\n",
                StandardCharsets.UTF_8);

        List<PublicDataParser.VolunteerRow> rows = PublicDataParser.parseVolunteers(csv);

        assertThat(rows).hasSize(1);
        PublicDataParser.VolunteerRow r = rows.get(0);
        assertThat(r.volunteerId()).isEqualTo("VOL-1");
        assertThat(r.title()).isEqualTo("하천 정화, 플로깅");
        assertThat(r.participantCount()).isEqualTo(2);
        assertThat(r.facilityId()).isEqualTo("FAC-1");
        assertThat(r.sido()).isEqualTo("경남");
        assertThat(r.sigungu()).isEqualTo("밀양시");
        assertThat(r.activityDate()).isEqualTo(LocalDate.of(2021, 12, 31));
        assertThat(r.startTime()).isEqualTo("14:00");
    }

    @Test
    void 날짜는_두_형식_모두_읽는다() {
        assertThat(PublicDataParser.toDate("2016-11-14")).isEqualTo(LocalDate.of(2016, 11, 14));
        assertThat(PublicDataParser.toDate("20050418")).isEqualTo(LocalDate.of(2005, 4, 18));
        assertThat(PublicDataParser.toDate("abc")).isNull();
    }
}
