package wavegen.misc

import chisel3._
import chisel3.util._
import wavegen.Util

case class OscilloscopeOpts(sampleCount: Int, sampleWidth: Int, width: Int, height: Int, scale: Int, xOffset: Int, yOffset: Int, xWidth: Int = 11, yWidth: Int = 10)

class OscilloDebug(opts: OscilloscopeOpts) extends Bundle {
	val state     = UInt(2.W)
	val fwPointer = Output(UInt(log2Ceil(opts.width * opts.height).W))
	val frPointer = Output(UInt(log2Ceil(opts.width * opts.height).W))
	val xPointer  = Output(UInt(log2Ceil(opts.width).W))
	val yPointer  = Output(UInt(log2Ceil(opts.height).W))
}

object OscilloDebug {
	def apply(opts: OscilloscopeOpts) = new OscilloDebug(opts)
}

class Oscilloscope(opts: OscilloscopeOpts) extends Module {
	require(opts.sampleCount >= opts.width / opts.scale)
	require(opts.height % (1 << opts.sampleWidth) == 0)
	require(opts.width  % opts.scale == 0)
	require(opts.height % opts.scale == 0)

	override val desiredName = s"Oscilloscope_${opts.sampleCount}sc${opts.sampleWidth}sw${opts.width}w${opts.height}h${opts.scale}s${opts.xOffset}xo${opts.yOffset}yo${opts.xWidth}xw${opts.yWidth}yw"

	val addrWidth = log2Ceil(opts.sampleCount)

	val io = IO(new Bundle {
		// val start    = Input(Bool())
		val x        = Input(UInt(opts.xWidth.W))
		val y        = Input(UInt(opts.yWidth.W))
		val trigger  = Input(UInt(opts.sampleWidth.W))
		val slope    = Input(Bool()) // true → positive slope, false → negative slope
		// val data     = Input(UInt(opts.sampleWidth.W))
		val sampleIn = Flipped(Valid(UInt(opts.sampleWidth.W)))
		// val addr     = Output(UInt(addrWidth.W))
		val out      = Valid(Bool())
		val debug    = Output(OscilloDebug(opts))
	})

	val sampleMemory = SyncReadMem(opts.sampleCount * 2, UInt(opts.sampleWidth.W))
	val addrReg = RegInit(0.U(addrWidth.W))
	val addr    = WireInit(addrReg)

	//  Accepting: Accepting sample data
	//  Rendering: Rendering pixel data to the framebuffer
	// Displaying: Outputting pixel data from the framebuffer
	val sAccepting :: sRendering :: sDisplaying :: Nil = Enum(3)

	val state        = RegInit(sAccepting)
	// val prevSample   = Mux(addrReg === 0.U, 0.U, sampleMemory(addrReg - 1.U))
	val prevSample   = RegInit(0.U(opts.sampleWidth.W))
	val triggerIndex = RegInit(0.U(addrWidth.W))

	io.out.valid := false.B
	io.out.bits  := DontCare

	val trigger = io.trigger
	val sample  = sampleMemory(addr)

	val framebuffer = SyncReadMem((opts.width / opts.scale) * (opts.height / opts.scale), Bool())
	val xPointer = RegInit(0.U(log2Ceil(opts.width).W))
	val yPointer = RegInit(0.U(log2Ceil(opts.height).W))
	val fwPointer = RegInit(0.U(log2Ceil(opts.width * opts.height).W))
	val frPointer = RegInit(0.U(log2Ceil(opts.width * opts.height).W))

	def getSample(xPixel: UInt): Unit = {
		val nextSampleIndex = Util.divide(xPixel, opts.scale) - triggerIndex
		addrReg    := nextSampleIndex
		addr       := nextSampleIndex
		prevSample := sample
	}

	when (state === sAccepting) {

		xPointer := 0.U
		yPointer := 0.U
		fwPointer := 0.U

		when (io.sampleIn.valid) {
			sampleMemory.write(addrReg, io.sampleIn.bits)

			when (addr <= opts.sampleCount.U && trigger =/= 0.U && ((io.slope && prevSample < trigger && trigger <= sample) || (!io.slope && sample <= trigger && trigger < prevSample))) {
				triggerIndex := addr
			}

			// prevSample := sample

			when (addrReg >= (opts.sampleCount - 1).U) {
				addrReg := 0.U
				state   := sRendering
			} .otherwise {
				addrReg := addrReg + 1.U
			}
		}

	} .elsewhen (state === sRendering) {

		// If height is 48 and sampleWidth is 4 (2^4 = 16), divide the y value by 48/16 = 3.
		val adjustedY = Util.divide((opts.height - 1).U - yPointer, opts.height / (1 << opts.sampleWidth))
		val sampleMatch = adjustedY === sample

		when (xPointer === 0.U) {
			framebuffer.write(fwPointer, sampleMatch)
		} .otherwise {
			framebuffer.write(fwPointer, sampleMatch || (prevSample <= adjustedY && adjustedY <= sample) || (prevSample >= adjustedY && adjustedY >= sample))
		}

		fwPointer := fwPointer + 1.U

		when (xPointer === (opts.width / opts.scale - 1).U) {
			xPointer := 0.U
			getSample(0.U)
			when (yPointer === (opts.height / opts.scale - 1).U) {
				yPointer := 0.U
				fwPointer := 0.U
				state    := sDisplaying
			} .otherwise {
				yPointer := yPointer + 1.U
			}
		} .otherwise {
			xPointer := xPointer + 1.U
			getSample(xPointer + 1.U)
		}

	}

	val adjustedX = Util.divide(io.x - opts.xOffset.U, opts.scale)
	val adjustedY = Util.divide(io.y - opts.yOffset.U, opts.scale)
	val nextAdjustedY =
		if (opts.yOffset == 0)
			Util.divide(io.y - opts.yOffset.U + 1.U, opts.scale)
		else
			Util.divide(io.y - (opts.yOffset - 1).U, opts.scale)

	when (Monitor(adjustedX)) {
		when (io.x < (opts.xOffset + opts.width).U) {
			// When the downscaled x value changes but we haven't reached the end of the row, increment the frame pointer.
			frPointer := frPointer + 1.U
		} .elsewhen (io.x === (opts.xOffset + opts.width).U) {
			// When the downscaled x value changes and we're past the end of the row, we either increase the frame
			// pointer as normal if the downscaled y value is about to change or set it back to the beginning of the
			// row if it's not. We need to be sure in the latter case to transition back to the accepting state if
			// we're done with the rendering for this frame.
			when (adjustedY === nextAdjustedY) {
				frPointer := frPointer - (opts.width / opts.scale - 1).U
			} .elsewhen (frPointer >= ((opts.width / opts.scale) * (opts.height / opts.scale) - 1).U && state === sDisplaying) {
				triggerIndex := 0.U
				state        := sAccepting
			} .otherwise {
				frPointer := frPointer + 1.U
			}
		}
	}

	when (opts.xOffset.U <= io.x && io.x < (opts.xOffset + opts.width).U && opts.yOffset.U <= io.y && io.y < (opts.yOffset + opts.height).U) {
		io.out.valid := true.B
		io.out.bits  := framebuffer.read(frPointer)

		// val adjustedY = Util.divide(opts.height.U - (io.y - opts.yOffset.U), opts.height / (1 << opts.sampleWidth))

		// io.out.valid := true.B
		// io.out.bits  := adjustedY === sample || (prevSample <= adjustedY && adjustedY <= sample) || (prevSample >= adjustedY && adjustedY >= sample)
	}

	io.debug.state     := state
	io.debug.fwPointer := fwPointer
	io.debug.frPointer := frPointer
	io.debug.xPointer  := xPointer
	io.debug.yPointer  := yPointer
}
