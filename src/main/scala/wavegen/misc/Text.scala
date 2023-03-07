package wavegen.misc

import chisel3._
import chisel3.util._
import scala.math.{sin, floor, Pi}

case class TextOpts(text: String, centerX: Boolean, centerY: Boolean, xOffset: Int, yOffset: Int, shift: Int)

class Text(opts: TextOpts, xWidth: Int = 11, yWidth: Int = 10, moduleName: String = "") extends Module {
	override val desiredName =
		if (moduleName.isEmpty())
			("Text_"
				+ opts.text.filter(_.isLetterOrDigit) + "_"
				+ opts.text.hashCode() + "_"
				+ (if (opts.centerX) "CX" else "")
				+ (if (opts.centerY) "CY" else "")
				+ opts.xOffset + "x"
				+ opts.yOffset + "y"
				+ opts.shift + "sh"
				+ xWidth + "xw"
				+ yWidth + "yw")
		else
			moduleName

	val io = IO(new Bundle {
		val x   = Input(UInt(xWidth.W))
		val y   = Input(UInt(yWidth.W))
		val out = Output(Bool())
	})

	val font   = Module(new wavegen.presentation.Font)
	val shift  = opts.shift
	val text   = VecInit(opts.text.toList.map(_.U(8.W)))
	val width  = text.length * 8
	val height = 8
	val left   = opts.xOffset
	val top    = opts.yOffset
	val x      = (
		if (opts.centerX)
			io.x - (left - (width << (shift - 1))).U
		else
			io.x - left.U
	) >> shift.U
	val charIndex = x >> 3.U
	val char      = text(charIndex)

	val y = (
		if (opts.centerY)
			(io.y - (top - (height << (shift - 1))).U)
		else
			(io.y - top.U)
	) >> shift.U

	// The RegNexts here are a WNS hack that appears to have no impact on the visuals.
	font.io.char := RegNext(char)
	font.io.x    := RegNext(x(2, 0))
	font.io.y    := RegNext(y(2, 0))

	io.out := 0.U <= x && x < width.U && 0.U <= y && y < height.U && font.io.out
}
