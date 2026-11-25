package com.wakeuphero.app

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.wakeuphero.core.TextBox

// S8-2 근무표 사진 → 글자
// ML Kit(휴대폰 안 글자 인식)이 사진에서 단어와 위치(상자)를 찾아 돌려준다.
// 해석은 core의 parseRosterBoxes가 한다 (머리글 날짜 칸 위치에 맞춰 근무를 넣음).
object RosterPhotoReader {

    fun read(context: Context, uri: Uri, onResult: (Result<List<TextBox>>) -> Unit) {
        val image = try {
            loadImage(context, uri)
        } catch (e: Exception) {
            onResult(Result.failure(e)); return
        }
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        recognizer.process(image)
            .addOnSuccessListener { text ->
                // 줄(line) 대신 단어(element) 단위로 꺼내야 표의 칸이 섞이지 않는다
                val boxes = text.textBlocks.flatMap { it.lines }.flatMap { it.elements }.mapNotNull { e ->
                    val b = e.boundingBox ?: return@mapNotNull null
                    TextBox(e.text, b.left, b.top, b.right, b.bottom)
                }
                onResult(Result.success(boxes))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
            .addOnCompleteListener { recognizer.close() }
    }

    // 작은 사진(단톡방 캡처 등)은 글자가 작아서 한 글자 칸(E, N)을 자주 놓친다
    // → 긴 변이 TARGET_SIZE가 되도록 최대 3배까지 키워서 읽는다 (에뮬레이터 시험: 14칸 중 10칸 → 13칸)
    // ImageDecoder는 사진의 회전 정보도 반영한다. Android 8(API 26·27)은 키우지 않고 그대로 읽음
    private const val TARGET_SIZE = 2500f

    private fun loadImage(context: Context, uri: Uri): InputImage {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return InputImage.fromFilePath(context, uri)
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // ML Kit이 픽셀을 읽을 수 있게
            val longSide = maxOf(info.size.width, info.size.height)
            val scale = (TARGET_SIZE / longSide).coerceIn(1f, 3f)
            if (scale > 1f) decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
        }
        return InputImage.fromBitmap(bitmap, 0)
    }
}
