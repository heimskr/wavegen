package wavegen.misc

import chisel3._
import chisel3.util._

class ImageOutput extends Module {
	val imageWidth  = 160
	val imageHeight = 144

	val io = IO(new Bundle {
		val x     = Input(UInt(11.W))
		val y     = Input(UInt(10.W))
		val rom   = Input(UInt(4.W))
		val addr  = Output(UInt(15.W))
		val red   = Output(UInt(8.W))
		val green = Output(UInt(8.W))
		val blue  = Output(UInt(8.W))
	})

	io.red   := 0.U
	io.green := 0.U
	io.blue  := 0.U

	val hueClocker = Module(new wavegen.StaticClocker(50, 74_250_000, "HueClocker"))
	hueClocker.io.enable := true.B

	val (hue, hueWrap) = Counter(0 to 255, hueClocker.io.tick)

	val region = RegInit(0.U(3.W))
	region := hue / 43.U

	val remainder = RegInit(0.U(9.W))
	remainder := (hue - (region * 43.U)) * 6.U

	val v = 255.U
	val p = 0.U
	val q = RegInit(0.U(8.W))
	q := (255.U * (255.U - ((255.U * remainder) >> 8.U))) >> 8.U

	val t = RegInit(0.U(8.W))
	t := (255.U * (255.U - ((255.U * (255.U - remainder)) >> 8.U))) >> 8.U

	switch (region) {
		is (0.U) { io.red := v; io.green := t; io.blue := p }
		is (1.U) { io.red := q; io.green := v; io.blue := p }
		is (2.U) { io.red := p; io.green := v; io.blue := t }
		is (3.U) { io.red := p; io.green := q; io.blue := v }
		is (4.U) { io.red := t; io.green := p; io.blue := v }
		is (5.U) { io.red := v; io.green := p; io.blue := q }
	}

	val x = Cat(0.U(2.W), io.x >> 2.U).asSInt - 80.S
	val y = Cat(0.U(2.W), io.y >> 2.U).asSInt - 18.S

	val palette = VecInit("h000000".U, "h090725".U, "h09081e".U, "h721f18".U, "hdb0026".U, "hdf0861".U, "h005a0e".U,
	                      "h005d02".U, "h4e4dc3".U, "h4e4ebd".U, "h209d06".U, "h1ca000".U, "hc27551".U, "hefac2e".U,
	                      "hf2ddd7".U, "hf8f8f8".U)

	// Might mess up the top left pixel.
	io.addr := (x + y * imageWidth.S).asUInt

	when (0.S <= x && x < imageWidth.S && 0.S <= y && y < imageHeight.S) {
		val color = palette(io.rom)
		io.red   := color(23, 16)
		io.green := color(15,  8)
		io.blue  := color( 7,  0)
	}
}
