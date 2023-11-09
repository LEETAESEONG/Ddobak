package com.ddobak.font.controller;

import com.ddobak.font.dto.request.MakeFontRequest;
import com.ddobak.font.dto.response.FontDetailResponse;
import com.ddobak.font.dto.response.FontListWithCountResponse;
import com.ddobak.font.dto.response.FontResponse;
import com.ddobak.font.entity.Font;
import com.ddobak.font.exception.InvalidFileFormatException;
import com.ddobak.font.service.FontImageService;
import com.ddobak.font.service.FontService;
import com.ddobak.security.util.LoginInfo;
import com.ddobak.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/font")
@RequiredArgsConstructor
@Slf4j
public class FontController {

    private final FontImageService fontImageService;
    private final FontService fontService;
    private final TransactionService transactionService;

    @GetMapping(value="/test")
    @Operation(summary = "테스트", description = "테스트하는 api 입니다.")
    @ApiResponse(responseCode = "200", description = "리턴 값으로 test를 반환합니다.")
    public ResponseEntity<String> test(@AuthenticationPrincipal LoginInfo loginInfo){
        return ResponseEntity.ok("test");
    }

    @PostMapping(value = "/sort",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "정렬api", description = "이미지정렬 api 입니다.")
    @ApiResponse(responseCode = "200", description = "리턴 값으로 s3Url을 반환합니다.")
    public ResponseEntity<String> sort(
            @Parameter(description = "multipart/form-data 형식의 이미지 리스트를 input으로 받습니다. 이때 key 값은 multipartFile 입니다.")
            @RequestPart("kor_file") MultipartFile kor_file,
            @RequestPart("eng_file") MultipartFile eng_file) {
        try {
            List<MultipartFile> files = Arrays.asList(kor_file, eng_file);
            String s3Urls = fontImageService.processAndUploadImages(files);

            return ResponseEntity.ok(s3Urls);
        } catch (InvalidFileFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed ㅠㅠ");
        }
    }
//    @CrossOrigin(origins = "http://localhost:3000") // React 앱의 URL을 허용합니다.
    @PostMapping(value = "/watch")
    @Operation(summary = "미리보기", description = "미리보기 api 입니다.")
    @ApiResponse(responseCode = "200", description = "리턴 값으로 zip파일을 반환합니다.")
    public ResponseEntity<byte[]> watchImage(@RequestParam(value = "sortUrl") String reqUrl,
                                             @AuthenticationPrincipal LoginInfo loginInfo){
        try {
            byte[] zipBytes = fontImageService.createZipFromUrls(reqUrl);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "files.zip");

            return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/goSetting")
    @Operation(summary = "폰트 세팅으로 이동", description = "초기 세팅하는 api 입니다.")
    @ApiResponse(responseCode = "200", description = "리턴 값으로 success를 반환합니다.")
    public ResponseEntity<String> createFont(@RequestParam("sortUrl") String font_sort_url,
                                           @AuthenticationPrincipal LoginInfo loginInfo) {
        fontService.createFont(font_sort_url,loginInfo);
        return ResponseEntity.ok("success");
    }

    @PostMapping(value = "/make")
    @Operation(summary = "폰트 제작", description = "폰트 제작하는 api 입니다.")
    @ApiResponse(responseCode = "200", description = "리턴 값으로 success를 반환합니다.")
    public ResponseEntity<String> makeFont(@RequestBody MakeFontRequest req,
                                           @AuthenticationPrincipal LoginInfo loginInfo) throws IOException {
        try {

            String fontUrl = fontImageService.createFont(req, loginInfo);
            Font makedFont = fontService.makeFont(req,loginInfo,fontUrl);
            transactionService.requestFontTransaction(makedFont, loginInfo.id(),makedFont.getPrice());
            return ResponseEntity.ok("success");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Font creation failed due to an internal error.");
        }
    }

    @GetMapping(value = "/list")
    @Operation(summary = "폰트 목록", description = "폰트 목록 조회하는 api입니다.")
    @ApiResponse(responseCode = "200", description = "리턴값으로 폰트목록에 필요한 값 리턴합니다.")
    public ResponseEntity<FontListWithCountResponse> getFontList(@AuthenticationPrincipal LoginInfo loginInfo,@PageableDefault(size=12) Pageable pageable,@RequestParam(required = false) String search, @RequestParam(required = false) List<String> keywords, @RequestParam(required = false) String freeCheck){

        Boolean free = null;
        if(freeCheck != null){
            if(freeCheck.equals("true")) {
                free = true;
            }
            else{
                free = false;
            }
        }

        List<FontResponse> fontList = fontService.getFontList(loginInfo,pageable,search,keywords,free);
        FontListWithCountResponse result = new FontListWithCountResponse(fontList.stream().count(),fontList);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/list/NoAuth")
    @Operation(summary = "폰트 목록", description = "폰트 목록 조회하는 api입니다.")
    @ApiResponse(responseCode = "200", description = "리턴값으로 폰트목록에 필요한 값 리턴합니다.")
    public ResponseEntity<FontListWithCountResponse> getFontList(@PageableDefault(size=12) Pageable pageable,@RequestParam(required = false) String search, @RequestParam(required = false) List<String> keywords, @RequestParam(required = false) String freeCheck){

        Boolean free = null;
        if(freeCheck != null){
            if(freeCheck.equals("true")) {
                free = true;
            }
            else{
                free = false;
            }
        }

        List<FontResponse> fontList = fontService.getFontListNoAuth(pageable,search,keywords,free);
        FontListWithCountResponse result = new FontListWithCountResponse(fontList.stream().count(),fontList);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/detail/{fontId}")
    @Operation(summary = "폰트 디테일",  description = "폰트 디테일을 조회하는 api입니다.")
    @ApiResponse(responseCode = "200", description = "리턴값으로 조회한 폰트의 디테일 값을 리턴합니다.")
    public ResponseEntity<FontDetailResponse> getFontDetail(@AuthenticationPrincipal LoginInfo loginInfo, @PathVariable Long fontId){
        System.out.println("######## 되나?");
        FontDetailResponse result = fontService.getFontDetail(fontId, loginInfo);
        return ResponseEntity.ok(result);
    }

}