package wavegen.misc

import chisel3._
import chisel3.util._
import wavegen.Util

case class OscilloscopeOpts(sampleCount: Int, sampleWidth: Int, width: Int, height: Int, scale: Int, xOffset: Int, yOffset: Int, xWidth: Int = 11, yWidth: Int = 10)

class Oscilloscope(opts: OscilloscopeOpts) extends Module {
	require(opts.sampleCount >= opts.width / opts.scale)
	require(opts.height % (1 << opts.sampleWidth) == 0)

	override val desiredName = s"Oscilloscope_${opts.sampleCount}sc${opts.sampleWidth}sw${opts.width}w${opts.height}h${opts.scale}s${opts.xOffset}xo${opts.yOffset}yo${opts.xWidth}xw${opts.yWidth}yw"

	val addrWidth = log2Ceil(opts.sampleCount)

	val io = IO(new Bundle {
		val start   = Input(Bool())
		val x       = Input(UInt(opts.xWidth.W))
		val y       = Input(UInt(opts.yWidth.W))
		val trigger = Input(UInt(opts.sampleWidth.W))
		val slope   = Input(Bool()) // true → positive slope, false → negative slope
		val data    = Input(UInt(opts.sampleWidth.W))
		val addr    = Output(UInt(addrWidth.W))
		val out     = Valid(Bool())
		val state   = Output(UInt(2.W))
	})

	//      Idle: Waiting to be started
	// Searching: Looking for a sample that matches the trigger value
	// Rendering: Outputting pixel data
	val sIdle :: sSearching :: sRendering :: Nil = Enum(3)

	val state        = RegInit(sIdle)
	val sampleIndex  = RegInit(0.U(addrWidth.W))
	val prevSample   = RegInit(0.U(opts.sampleWidth.W))
	val triggerIndex = RegInit(0.U(addrWidth.W))

	io.addr      := sampleIndex
	io.out.valid := false.B
	io.state     := state

	val trigger = io.trigger
	val sample  = io.data

	when (state === sIdle) {
		sampleIndex  := 0.U
		triggerIndex := 0.U
		when (io.start) {
			state := sSearching
		}
	} .elsewhen (state === sSearching) {
		sampleIndex := sampleIndex + 1.U
		when (io.slope) {
			when (prevSample < trigger && trigger <= sample) {
				triggerIndex := sampleIndex
				state        := sRendering
			}
		} .otherwise {
			when (sample <= trigger && trigger < prevSample) {
				triggerIndex := sampleIndex
				state        := sRendering
			}
		}
	} .elsewhen (state === sRendering) {
		// TODO: timing issues? Data will be delayed by one cycle.
		val shownSampleIndex = Util.divide(io.x - opts.xOffset.U, opts.scale) - triggerIndex
		sampleIndex := shownSampleIndex
		io.addr     := shownSampleIndex
		prevSample  := sample

		when (opts.xOffset.U <= io.x && io.x < (opts.xOffset + opts.width).U && opts.yOffset.U <= io.y && io.y < (opts.yOffset + opts.height).U) {
			// If height is 48 and sampleWidth is 4 (2^4 = 16), divide the y value by 48/16 = 3.
			val adjustedY = Util.divide(opts.height.U - (io.y - opts.yOffset.U), opts.height / (1 << opts.sampleWidth))

			io.out.valid := true.B
			io.out.bits  := adjustedY === sample || (prevSample <= adjustedY && adjustedY <= sample) || (prevSample >= adjustedY && adjustedY >= sample)
		} .elsewhen ((opts.xOffset + opts.width).U <= io.x) {
			// Show's over
			state := sIdle
		}
	}
}
