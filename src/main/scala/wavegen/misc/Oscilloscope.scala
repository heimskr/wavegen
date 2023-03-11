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
		val start    = Input(Bool())
		val x        = Input(UInt(opts.xWidth.W))
		val y        = Input(UInt(opts.yWidth.W))
		val trigger  = Input(UInt(opts.sampleWidth.W))
		val slope    = Input(Bool()) // true → positive slope, false → negative slope
		// val data     = Input(UInt(opts.sampleWidth.W))
		val sampleIn = Input(UInt(opts.sampleWidth.W))
		// val addr     = Output(UInt(addrWidth.W))
		val out      = Valid(Bool())
		val state    = Output(UInt(2.W))
	})

	//       Idle: Waiting to be started
	//  Searching: Looking for a sample that matches the trigger value
	//  Rendering: Rendering pixel data to the framebuffer
	// Displaying: Outputting pixel data from the framebuffer
	val sIdle :: sSearching :: sRendering :: sDisplaying :: Nil = Enum(4)

	val state        = RegInit(sIdle)
	val sampleIndex  = RegInit(0.U(addrWidth.W))
	val prevSample   = RegInit(0.U(opts.sampleWidth.W))
	val triggerIndex = RegInit(0.U(addrWidth.W))

	// io.addr      := sampleIndex
	io.out.valid := false.B
	io.state     := state

	val trigger = io.trigger
	val sample  = io.data

	val framebuffer = SyncReadMem(opts.width * opts.height, Bool())
	val xPointer = RegInit(0.U(log2Ceil(opts.width).W))
	val yPointer = RegInit(0.U(log2Ceil(opts.height).W))
	val fPointer = RegInit(0.U(log2Ceil(opts.width * opts.height).W))

	def getSample(xPixel: UInt): Unit = {
		val nextSampleIndex = Util.divide(xPixel, opts.scale) - triggerIndex
		sampleIndex := nextSampleIndex
		io.addr     := nextSampleIndex
		prevSample  := sample
	}

	when (state === sIdle) {

		sampleIndex  := 0.U
		triggerIndex := 0.U
		xPointer := 0.U
		yPointer := 0.U
		fPointer := 0.U
		when (io.start) {
			state := sSearching
		}

	} .elsewhen (state === sSearching) {

		sampleIndex := sampleIndex + 1.U
		when (io.slope) {
			when (prevSample < trigger && trigger <= sample) {
				triggerIndex := sampleIndex
				state        := sRendering
				getSample(0.U)
			}
		} .otherwise {
			when (sample <= trigger && trigger < prevSample) {
				triggerIndex := sampleIndex
				state        := sRendering
				getSample(0.U)
			}
		}

	} .elsewhen (state === sRendering) {
		// If height is 48 and sampleWidth is 4 (2^4 = 16), divide the y value by 48/16 = 3.
		val adjustedY = Util.divide(opts.height.U - yPointer, opts.height / (1 << opts.sampleWidth))
		val sampleMatch = adjustedY === sample

		when (0.U < xPointer) {
			framebuffer.write(fPointer, sampleMatch || (prevSample <= adjustedY && adjustedY <= sample) || (prevSample >= adjustedY && adjustedY >= sample))
		} .otherwise {
			framebuffer.write(fPointer, sampleMatch)
		}

		fPointer := fPointer + 1.U

		when (xPointer +& 1.U === opts.width.U) {
			xPointer := 0.U
			getSample(0.U)
			when (yPointer +& 1.U === opts.height.U) {
				yPointer := 0.U
				fPointer := 0.U
				state    := sDisplaying
			} .otherwise {
				yPointer := yPointer + 1.U
			}
		} .otherwise {
			xPointer := xPointer + 1.U
			getSample(xPointer + 1.U)
		}

	} .elsewhen (state === sDisplaying) {
		val adjustedX = Util.divide(io.x - opts.xOffset.U, opts.scale)

		// Increase framebuffer pointer when io.x increases after having been in the visible range
		when (Monitor(adjustedX) && io.x <= (opts.xOffset + opts.width).U) {
			fPointer := fPointer + 1.U
		}

		when (opts.xOffset.U <= io.x && io.x < (opts.xOffset + opts.width).U && opts.yOffset.U <= io.y && io.y < (opts.yOffset + opts.height).U) {
			io.out.valid := true.B
			io.out.bits  := framebuffer.read(fPointer)

			// val adjustedY = Util.divide(opts.height.U - (io.y - opts.yOffset.U), opts.height / (1 << opts.sampleWidth))

			// io.out.valid := true.B
			// io.out.bits  := adjustedY === sample || (prevSample <= adjustedY && adjustedY <= sample) || (prevSample >= adjustedY && adjustedY >= sample)
		}

		when (io.start) {
			state := sIdle
		}

	}
}
