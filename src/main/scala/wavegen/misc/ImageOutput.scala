package wavegen.misc

import chisel3._
import chisel3.util._
import scala.math.{sin, floor, Pi}

class ImageOutput(implicit clockFreq: Int) extends Module {
	val imageWidth  = 160
	val imageHeight = 144

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
	val slide = RegInit(0.U(8.W))

	slideshow.io.slide := 0.U
	slideshow.io.x := io.x
	slideshow.io.y := io.y

	val leftOn  = RegInit(false.B)
	val rightOn = RegInit(false.B)

	val maxSlides = 16

	when (io.pulseL) {
		slide := Mux(slide === 0.U, maxSlides.U, slide - 1.U)
	}

	when (io.pulseR) {
		slide := Mux(slide < maxSlides.U, slide + 1.U, 0.U)
	}

	io.addr := 0.U

	when (slide === 0.U) {
		val hueClocker = Module(new wavegen.StaticClocker(50, 74_250_000, true, "HueClocker"))
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

		val palette = VecInit("h000000".U, "h090725".U, "h09081e".U, "h721f18".U, "hdb0026".U, "hdf0861".U, "h005a0e".U,
		                      "h005d02".U, "h4e4dc3".U, "h4e4ebd".U, "h209d06".U, "h1ca000".U, "hc27551".U, "hefac2e".U,
		                      "hf2ddd7".U, "hf8f8f8".U)

		io.addr := (x + y * imageWidth.S).asUInt

		when (0.S <= x && x < imageWidth.S && 0.S <= y && y < imageHeight.S) {
			val color = palette(io.rom)
			io.red   := color(23, 16)
			io.green := color(15,  8)
			io.blue  := color( 7,  0)
		}

		{
			val undulate = Module(new wavegen.StaticClocker(32, 74_250_000, true, "UndulationClocker"))
			undulate.io.enable := true.B
			val font      = Module(new wavegen.presentation.Font)
			val shift     = 3
			val text      = VecInit("Game Boy".toList.map(_.U(8.W)))
			val width     = text.length * 8
			val height    = 8
			val top       = 16
			val bottom    = top + height
			val x         = (io.x - ((1280 - (width << shift)) / 2).U) >> shift.U
			val charIndex = x >> 3.U
			val char      = text(charIndex)

			val resolution = 128
			val sines = VecInit.tabulate(resolution)(x => floor((sin(x * 8 * Pi / resolution) + 1) * (4 << shift)).toInt.U)

			val counter = RegInit(0.U(log2Ceil(resolution - text.length).W))
			when (undulate.io.tick) {
				when (counter >= (resolution - 1).U) {
					counter := 0.U
				} .otherwise {
					counter := counter + 1.U
				}
			}

			val y = (io.y - top.U - sines((counter + charIndex)(6, 0))) >> shift.U

			font.io.char := char
			font.io.x    := x(2, 0)
			font.io.y    := y(2, 0)

			when (0.U <= x && x < width.U && 0.U <= y && y < height.U && font.io.out) {
				io.green := 255.U - colors.io.green
				io.red   := 255.U - colors.io.red
				io.blue  := 255.U - colors.io.blue
			}
		}
	} .otherwise {
		slideshow.io.slide := slide - 1.U
		io.red   := slideshow.io.red
		io.green := slideshow.io.green
		io.blue  := slideshow.io.blue
	}
}
