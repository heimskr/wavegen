package wavegen.misc

import chisel3._
import chisel3.util._

class ImageOutput extends Module {
	val imageWidth  = 160
	val imageHeight = 144

	val io = IO(new Bundle {
		val x     = Input(UInt(11.W))
		val y     = Input(UInt(11.W))
		val rom   = Input(UInt(4.W))
		val addr  = Output(UInt(15.W))
		val red   = Output(UInt(8.W))
		val green = Output(UInt(8.W))
		val blue  = Output(UInt(8.W))
	})

	io.red   := 0.U
	io.green := 0.U
	io.blue  := 0.U

	val x = Cat(0.U(2.W), io.x >> 2.U).asSInt - 80.S
	val y = Cat(0.U(2.W), io.y >> 2.U).asSInt - 18.S

	val palette = VecInit("h000000".U, "h090725".U, "h09081e".U, "h721f18".U, "hdb0026".U, "hdf0861".U, "h005a0e".U,
	                      "h005d02".U, "h4e4dc3".U, "h4e4ebd".U, "h209d06".U, "h1ca000".U, "hc27551".U, "hefac2e".U,
	                      "hf2ddd7".U, "hf8f8f8".U)

	// Might mess up the top left pixel.
	io.addr := (x + y * imageWidth.S + 1.S).asUInt

	when (0.S <= x && x < imageWidth.S && 0.S <= y && y < imageHeight.S) {
		val color = palette(io.rom)
		io.red   := color(23, 16)
		io.green := color(15, 8)
		io.blue  := color(7, 0)
	}
}
