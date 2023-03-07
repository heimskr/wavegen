package wavegen.misc

import chisel3._
import chisel3.util._
import scala.math.{sin, floor, Pi}

class ImageOutput(val showScreenshot: Boolean = false) extends Module {
	val imageWidth   = 160
	val imageHeight  = 144
	val demoSlideGB  = 9
	val demoSlideNES = 11

	val io = IO(new Bundle {
		val x       = Input(UInt(11.W))
		val y       = Input(UInt(10.W))
		val sw      = Input(UInt(8.W))
		val rom     = Input(UInt(4.W))
		val pulseL  = Input(Bool())
		val pulseR  = Input(Bool())
		val addr    = Output(UInt(15.W))
		val red     = Output(UInt(8.W))
		val green   = Output(UInt(8.W))
		val blue    = Output(UInt(8.W))
	})

	val slideshow = Module(new wavegen.presentation.Slideshow)
	val slide     = RegInit(0.U(8.W))
	val useNES    = io.sw(0)

	slideshow.io.slide := slide
	slideshow.io.x     := io.x
	slideshow.io.y     := io.y

	val leftOn  = RegInit(false.B)
	val rightOn = RegInit(false.B)

	val maxSlides = 16

	when (io.pulseL) {
		slide := Mux(slide === 0.U, maxSlides.U, slide - 1.U)
	}

	when (io.pulseR) {
		slide := Mux(slide < maxSlides.U, slide + 1.U, 0.U)
	}

	io.addr := DontCare

	when (slide === demoSlideGB.U || slide === demoSlideNES.U) {
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
		io.red   := colors.io.red
		io.green := colors.io.green
		io.blue  := colors.io.blue

		val x = Cat(0.U(2.W), io.x >> 2.U).asSInt - 80.S
		val y = Cat(0.U(2.W), io.y >> 2.U).asSInt - 18.S

		if (showScreenshot) {
			val palette = VecInit("h000000".U, "h090725".U, "h09081e".U, "h721f18".U,
			                      "hdb0026".U, "hdf0861".U, "h005a0e".U, "h005d02".U,
			                      "h4e4dc3".U, "h4e4ebd".U, "h209d06".U, "h1ca000".U,
			                      "hc27551".U, "hefac2e".U, "hf2ddd7".U, "hf8f8f8".U)

			io.addr := (x + y * imageWidth.S).asUInt

			when (0.S <= x && x < imageWidth.S && 0.S <= y && y < imageHeight.S) {
				val color = palette(io.rom)
				io.red   := color(23, 16)
				io.green := color(15,  8)
				io.blue  := color( 7,  0)
			}
		}

		val xBase    = 1280 / 2
		val yBase    = 720 / 2
		val wShift   = 4
		val distance = 3 << (wShift - 2)
		val wC       = 6

		val demoActive = slide === demoSlideNES.U || slide === demoSlideGB.U


		when (slide === demoSlideNES.U) {
			if (showScreenshot) {
				val wavyBg = Module(new WavyText(WavyTextOpts(text="NES", centerX=true, centerY=true, xOffset=xBase + distance, yOffset=yBase + distance, shift=wShift, waveCoefficient=wC)))
				wavyBg.io.x := io.x
				wavyBg.io.y := io.y
				when (wavyBg.io.out) {
					io.red   := colors.io.red
					io.green := colors.io.green
					io.blue  := colors.io.blue
				}
			}

			val wavyMid = Module(new WavyText(WavyTextOpts(text="NES", centerX=true, centerY=true, xOffset=xBase + distance/2, yOffset=yBase + distance/2, shift=wShift, waveCoefficient=wC)))
			wavyMid.io.x := io.x
			wavyMid.io.y := io.y
			when (wavyMid.io.out) {
				io.red   := 0.U
				io.green := 0.U
				io.blue  := 0.U
			}

			val wavy = Module(new WavyText(WavyTextOpts(text="NES", centerX=true, centerY=true, xOffset=xBase, yOffset=yBase, shift=wShift, waveCoefficient=wC)))
			wavy.io.x := io.x
			wavy.io.y := io.y
			when (wavy.io.out) {
				io.red   := 255.U
				io.green := 255.U
				io.blue  := 255.U
			}

			when (!useNES) {
				val warning = Module(new Text(TextOpts(text="Flip switch 0!", centerX=false, centerY=false, xOffset=10, yOffset=720-10-(8<<2), shift=2)))
				warning.io.x := io.x
				warning.io.y := io.y
				when (warning.io.out) {
					io.red   := 0.U
					io.green := 0.U
					io.blue  := 0.U
				}
			}
		} .otherwise {
			if (showScreenshot) {
				val wavyBg = Module(new WavyText(WavyTextOpts(text="Game Boy", centerX=true, centerY=true, xOffset=xBase + distance, yOffset=yBase + distance, shift=wShift, waveCoefficient=wC)))
				wavyBg.io.x := io.x
				wavyBg.io.y := io.y
				when (wavyBg.io.out) {
					io.red   := colors.io.red
					io.green := colors.io.green
					io.blue  := colors.io.blue
				}
			}

			val wavyMid = Module(new WavyText(WavyTextOpts(text="Game Boy", centerX=true, centerY=true, xOffset=xBase + distance/2, yOffset=yBase + distance/2, shift=wShift, waveCoefficient=wC)))
			wavyMid.io.x := io.x
			wavyMid.io.y := io.y
			when (wavyMid.io.out) {
				io.red   := 0.U
				io.green := 0.U
				io.blue  := 0.U
			}

			val wavy = Module(new WavyText(WavyTextOpts(text="Game Boy", centerX=true, centerY=true, xOffset=xBase, yOffset=yBase, shift=wShift, waveCoefficient=wC)))
			wavy.io.x := io.x
			wavy.io.y := io.y
			when (wavy.io.out) {
				io.red   := 255.U
				io.green := 255.U
				io.blue  := 255.U
			}

			when (useNES) {
				val warning = Module(new Text(TextOpts(text="Flip switch 0!", centerX=false, centerY=false, xOffset=10, yOffset=720-10-(8<<2), shift=2)))
				warning.io.x := io.x
				warning.io.y := io.y
				when (warning.io.out) {
					io.red   := 0.U
					io.green := 0.U
					io.blue  := 0.U
				}
			}
		}

		when (slideshow.io.out) {
			io.red   := 0.U
			io.green := 0.U
			io.blue  := 0.U
		}
	} .otherwise {
		io.red   := slideshow.io.red
		io.green := slideshow.io.green
		io.blue  := slideshow.io.blue
	}
}
