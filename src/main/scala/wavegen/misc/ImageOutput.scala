package wavegen.misc

import chisel3._
import chisel3.util._
import scala.math.{sin, floor, Pi}
import wavegen._
import wavegen.presentation.Bar
import wavegen.presentation.Slideshow

class ImageOutput(val showScreenshot: Boolean = false) extends Module {
	implicit val clockFreq = 74_250_000

	val screenWidth  = 1280
	val screenHeight = 720
	val demoSlideGB  = 9
	val demoSlideNES = 11
	val maxSlides    = 16

	val io = IO(new Bundle {
		val audioClock  = Input(Clock())
		val x           = Input(UInt(11.W))
		val y           = Input(UInt(10.W))
		val sw          = Input(UInt(8.W))
		val pulseL      = Input(Bool())
		val pulseR      = Input(Bool())
		val red         = Output(UInt(8.W))
		val green       = Output(UInt(8.W))
		val blue        = Output(UInt(8.W))
		val nesButtons  = Input(wavegen.NESButtons())
		val useNES      = Input(Bool())
		val useNESOut   = Valid(Bool())
		val multiplier  = Input(UInt(5.W))
		val rxByte      = Flipped(Valid(UInt(8.W)))
		val gbChannels  = Input(Vec(4, UInt(4.W)))
		val nesChannels = Input(Vec(4, UInt(4.W)))
		val jb0 = Output(Bool())
		val jb1 = Output(Bool())
		val jb2 = Output(Bool())
		val jb3 = Output(Bool())
		val jb4 = Output(Bool())
	})

	val slideshow = Module(new Slideshow)
	val slide     = RegInit(0.U(8.W))
	val useNES    = io.useNES

	slideshow.io.slide := slide
	slideshow.io.x     := io.x
	slideshow.io.y     := io.y

	val goPrevious = io.pulseL || io.nesButtons.left  || (io.rxByte.valid && io.rxByte.bits === 'a'.U)
	val goNext     = io.pulseR || io.nesButtons.right || (io.rxByte.valid && io.rxByte.bits === 'd'.U)

	when (goPrevious) {
		slide := Mux(slide === 0.U, maxSlides.U, slide - 1.U)
	}

	when (goNext) {
		slide := Mux(slide < maxSlides.U, slide + 1.U, 0.U)
	}

	val hueClock = StaticClocker(50, clockFreq, true)

	val waves = 2
	val amplitude = 100
	val table = VecInit.tabulate(screenWidth)(x => floor((1 + sin(2 * x * Pi / screenWidth * waves)) / 2 * amplitude).intValue().U(10.W))
	val shift = table(io.x)

	val (hue, hueWrap) = Counter(0 to 255, hueClock)
	val adjustedHue = (hue + (io.x >> 2.U) + (io.y >> 3.U) - shift)(7, 0)

	val colors = Module(new Colors)
	colors.io.hue := adjustedHue

	val isDemo = slide === demoSlideGB.U || slide === demoSlideNES.U
	io.useNESOut.valid := isDemo
	io.useNESOut.bits  := DontCare

	val fakeAudioClock = StaticClocker(48000, clockFreq)

	io.jb0 := 0.U
	io.jb1 := 0.U
	io.jb2 := 0.U
	io.jb3 := 0.U
	io.jb4 := 0.U

	when (isDemo) {
		io.red   := colors.io.red
		io.green := colors.io.green
		io.blue  := colors.io.blue

		val xBase    = screenWidth / 2
		val yBase    = screenHeight / 2
		val wShift   = 4
		val distance = 3 << (wShift - 2)
		val wC       = 6

		def setAll(value: Int): Unit = { io.red := value.U; io.green := value.U; io.blue := value.U }

		val barInnerWidth  = 256
		val barInnerHeight = 64
		val barStrokeWidth = 6
		val barMargin = 32
		val bar = Module(new Bar(screenWidth  - barInnerWidth  - 2 * barStrokeWidth - barMargin,
		                         screenHeight - barInnerHeight - 2 * barStrokeWidth - barMargin,
		                         barInnerWidth + 2 * barStrokeWidth, barInnerHeight + 2 * barStrokeWidth,
		                         barStrokeWidth, 5, (0, 0, 0), (128, 128, 128), (255, 255, 255)))
		bar.io.x     := io.x
		bar.io.y     := io.y
		bar.io.value := io.multiplier
		when (bar.io.out.valid) {
			when (bar.io.out.bits.red === 128.U) {
				io.red   := 255.U - colors.io.red
				io.green := 255.U - colors.io.green
				io.blue  := 255.U - colors.io.blue
			} .otherwise {
				io.red   := bar.io.out.bits.red
				io.green := bar.io.out.bits.green
				io.blue  := bar.io.out.bits.blue
			}
		}

		when (slide === demoSlideNES.U) {
			io.useNESOut.bits := true.B
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
		} .otherwise {
			io.useNESOut.bits := false.B

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
		}
	} .elsewhen (slide === 0.U && ((11 << 2) + (8 << 2)).U <= io.y && io.y < (screenHeight - 70).U && slideshow.io.red === 255.U) {
		io.red   := colors.io.red
		io.green := colors.io.green
		io.blue  := colors.io.blue
	} .otherwise {
		io.red   := slideshow.io.red
		io.green := slideshow.io.green
		io.blue  := slideshow.io.blue
	}
}
