package wavegen.misc

import chisel3._
import chisel3.util._
import scala.math.{sin, floor, Pi}

class ImageOutput extends Module {
	val imageWidth   = 160
	val imageHeight  = 144
	val demoSlideGB  = 9
	val demoSlideNES = 11

	val io = IO(new Bundle {
		val x       = Input(UInt(11.W))
		val y       = Input(UInt(10.W))
		val sw      = Input(UInt(8.W))
		val left    = Input(Bool())
		val right   = Input(Bool())
		val red     = Output(UInt(8.W))
		val green   = Output(UInt(8.W))
		val blue    = Output(UInt(8.W))
		val buttons = Input(wavegen.NESButtons())
		val useNES  = Valid(Output(Bool()))
	})

	val slideshow = Module(new wavegen.presentation.Slideshow)
	val slide     = RegInit(0.U(8.W))

	io.useNES.valid := false.B
	io.useNES.bits  := DontCare

	slideshow.io.slide := slide
	slideshow.io.x     := io.x
	slideshow.io.y     := io.y

	val maxSlides = 16

	when (io.left) {
		slide := Mux(slide === 0.U, maxSlides.U, slide - 1.U)
	}

	when (io.right) {
		slide := Mux(slide < maxSlides.U, slide + 1.U, 0.U)
	}

	val hueClocker = Module(new wavegen.StaticClocker(50, 74_250_000, true))
	hueClocker.io.enable := true.B

	val waves = 2
	val amplitude = 100
	val table = VecInit.tabulate(1280)(x => floor((1 + sin(2 * x * Pi / 1280 * waves)) / 2 * amplitude).intValue().U(10.W))
	val shift = table(io.x)

	val (hue, hueWrap) = Counter(0 to 255, hueClocker.io.tick)
	val adjustedHue = (hue + (io.x >> 2.U) + (io.y >> 3.U) - shift)(7, 0)

	val colors = Module(new Colors)
	colors.io.hue := adjustedHue

	when (slide === demoSlideGB.U || slide === demoSlideNES.U) {
		io.red   := colors.io.red
		io.green := colors.io.green
		io.blue  := colors.io.blue

		val xBase    = 1280 / 2
		val yBase    = 720 / 2
		val wShift   = 4
		val distance = 3 << (wShift - 2)
		val wC       = 6

		val demoActive = slide === demoSlideNES.U || slide === demoSlideGB.U
		io.useNES.valid := demoActive

		def setAll(value: Int): Unit = { io.red := value.U; io.green := value.U; io.blue := value.U }

		when (slide === demoSlideNES.U) {
			val wavyBg = Module(new WavyText(WavyTextOpts(text="NES", centerX=true, centerY=true, xOffset=xBase + distance, yOffset=yBase + distance, shift=wShift, waveCoefficient=wC)))
			wavyBg.io.x := io.x
			wavyBg.io.y := io.y
			when (wavyBg.io.out) {
				io.red   := 255.U - colors.io.red
				io.green := 255.U - colors.io.green
				io.blue  := 255.U - colors.io.blue
			}

			val topFg = Module(new Text(TextOpts(text="The Legend of Zelda (title screen)", centerX=true, centerY=false, xOffset=1280/2-2, yOffset=20-2, shift=2)))
			topFg.io.x := io.x
			topFg.io.y := io.y
			val topBg = Module(new Text(TextOpts(text="The Legend of Zelda (title screen)", centerX=true, centerY=false, xOffset=1280/2+2, yOffset=20+2, shift=2)))
			topBg.io.x := io.x
			topBg.io.y := io.y

			when (topBg.io.out) { setAll(0)   }
			when (topFg.io.out) { setAll(255) }

			val wavyMid = Module(new WavyText(WavyTextOpts(text="NES", centerX=true, centerY=true, xOffset=xBase + distance/2, yOffset=yBase + distance/2, shift=wShift, waveCoefficient=wC)))
			wavyMid.io.x := io.x
			wavyMid.io.y := io.y
			when (wavyMid.io.out) {
				setAll(0)
			}

			val wavy = Module(new WavyText(WavyTextOpts(text="NES", centerX=true, centerY=true, xOffset=xBase, yOffset=yBase, shift=wShift, waveCoefficient=wC)))
			wavy.io.x := io.x
			wavy.io.y := io.y
			when (wavy.io.out) {
				setAll(255)
			}

			io.useNES.bits  := true.B
		} .otherwise {
			val wavyBg = Module(new WavyText(WavyTextOpts(text="Game Boy", centerX=true, centerY=true, xOffset=xBase + distance, yOffset=yBase + distance, shift=wShift, waveCoefficient=wC)))
			wavyBg.io.x := io.x
			wavyBg.io.y := io.y
			when (wavyBg.io.out) {
				io.red   := 255.U - colors.io.red
				io.green := 255.U - colors.io.green
				io.blue  := 255.U - colors.io.blue
			}

			val topFg = Module(new Text(TextOpts(text="Pok\u0019mon Card GB2 (GR duel music)", centerX=true, centerY=false, xOffset=1280/2-2, yOffset=20-2, shift=2)))
			topFg.io.x := io.x
			topFg.io.y := io.y
			val topBg = Module(new Text(TextOpts(text="Pok\u0019mon Card GB2 (GR duel music)", centerX=true, centerY=false, xOffset=1280/2+2, yOffset=20+2, shift=2)))
			topBg.io.x := io.x
			topBg.io.y := io.y

			when (topBg.io.out) { setAll(0)   }
			when (topFg.io.out) { setAll(255) }

			val wavyMid = Module(new WavyText(WavyTextOpts(text="Game Boy", centerX=true, centerY=true, xOffset=xBase + distance/2, yOffset=yBase + distance/2, shift=wShift, waveCoefficient=wC)))
			wavyMid.io.x := io.x
			wavyMid.io.y := io.y
			when (wavyMid.io.out) {
				setAll(0)
			}

			val wavy = Module(new WavyText(WavyTextOpts(text="Game Boy", centerX=true, centerY=true, xOffset=xBase, yOffset=yBase, shift=wShift, waveCoefficient=wC)))
			wavy.io.x := io.x
			wavy.io.y := io.y
			when (wavy.io.out) {
				setAll(255)
			}

			io.useNES.bits  := false.B
		}
	} .elsewhen (slide === 0.U && ((11 << 2) + (8 << 2)).U <= io.y && io.y < (720 - 70).U && slideshow.io.red === 255.U) {
		io.red   := colors.io.red
		io.green := colors.io.green
		io.blue  := colors.io.blue
	} .otherwise {
		io.red   := slideshow.io.red
		io.green := slideshow.io.green
		io.blue  := slideshow.io.blue
	}
}
