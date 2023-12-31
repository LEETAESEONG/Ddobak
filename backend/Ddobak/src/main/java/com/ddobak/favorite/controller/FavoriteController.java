package com.ddobak.favorite.controller;

import com.ddobak.favorite.entity.Favorite;
import com.ddobak.favorite.service.FavoriteService;
import com.ddobak.font.dto.response.DibbedFontInfo;
import com.ddobak.font.entity.Font;
import com.ddobak.security.util.LoginInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/favorite") // changed to plural form to be more RESTful
public class FavoriteController {

    private final FavoriteService favoriteService;

    // 찜 확인
    @GetMapping("/check/{fontId}")
    public ResponseEntity<Boolean> checkDibExists(@PathVariable Long fontId, @AuthenticationPrincipal LoginInfo loginInfo) {
        Long memberId = loginInfo.id();
        boolean result = favoriteService.existsByMemberIDAndFontID(memberId, fontId);
        return ResponseEntity.ok(result);
    }

    // 찜 하기
    @PostMapping("/{fontId}")
    public ResponseEntity<Void> makeDib(@PathVariable Long fontId, @AuthenticationPrincipal LoginInfo loginInfo) {
        Long memberId = loginInfo.id();
        favoriteService.makeDib(memberId, fontId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 찜 삭제
    @DeleteMapping("/{fontId}")
    public ResponseEntity<Void> removeDib(@PathVariable Long fontId, @AuthenticationPrincipal LoginInfo loginInfo) {
        Long memberId = loginInfo.id();
        favoriteService.removeDib(memberId, fontId);
        return ResponseEntity.noContent().build();
    }

    // 폰트의 찜 개수
    @GetMapping("/count/{fontId}")
    public ResponseEntity<Long> getDibCountByFontId(@PathVariable Long fontId) {
        Long count = favoriteService.countByFontId(fontId);
        return ResponseEntity.ok(count);
    }

    // 찜 목록 가져 오기
    @GetMapping("/list")
    public ResponseEntity<List<DibbedFontInfo>> getDibsByMember(@AuthenticationPrincipal LoginInfo loginInfo) {
        Long memberId = loginInfo.id();
        List<Favorite> favorites = favoriteService.findByMemberId(memberId);

        if (favorites.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            List<DibbedFontInfo> result = new ArrayList<>();
            for (Favorite favorite : favorites) {
                Font font = favorite.getFont(); // 즐겨찾기된 폰트 엔티티를 가져오는 메소드
                DibbedFontInfo dibbedFontInfo = favoriteService.getDibbedFontInfo(font);
                result.add(dibbedFontInfo);
            }
            return ResponseEntity.ok(result);
        }
    }

}
