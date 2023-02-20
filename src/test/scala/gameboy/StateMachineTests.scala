package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import chisel3.stage._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files
import java.nio.file.Paths
import scala.util.control.Breaks._

class StateMachineTestModule(implicit clockFreq: Int) extends Module {
	implicit val inSimulator = true

	val io = IO(new Bundle {
		val start      = Input(Bool())
		val data       = Input(UInt(8.W))
		val state      = Output(UInt(4.W))
		val error      = Output(UInt(4.W))
		val errorInfo  = Output(UInt(8.W))
		val errorInfo2 = Output(UInt(16.W))
		val registers  = Output(Registers())
		val addr       = Output(UInt(18.W))
	})

	// val rom = RegNext(VecInit(Files.readAllBytes(Paths.get("worldmap.vgm")).take(2048).map(_.S(8.W).asUInt)))

	val addr = RegInit(0.U(18.W))

	val (counter, wrap) = Counter(0 until 2)

	val stateMachine = Module(new StateMachine(clockFreq))
	stateMachine.io.start := io.start
	stateMachine.io.tick  := wrap
	stateMachine.io.rom   := io.data
	addr := stateMachine.io.addr

	io.error := stateMachine.io.error
	io.errorInfo := stateMachine.io.errorInfo
	io.errorInfo2 := stateMachine.io.errorInfo2
	io.registers := stateMachine.io.registers
	io.addr := addr
	io.state := stateMachine.io.state
}

class StateMachineTests extends AnyFlatSpec with ChiselScalatestTester {
	implicit val clockFreq = 100_000_000

	(new ChiselStage).emitVerilog(new StateMachineTestModule)

	behavior of "StateMachine"
	it should "do something (50)" in {
		val rom = Files.readAllBytes(Paths.get("worldmap.fpb"))
		test(new StateMachineTestModule).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.clock.setTimeout(0)
			dut.clock.step()
			dut.io.start.poke(true)
			dut.clock.step()
			dut.io.start.poke(false)
			var oldAddr: Int = -1
			breakable {
				for (i <- 0 to 10000) {
					dut.clock.step()
					val addr = dut.io.addr.peek()
					val asInt = addr.litValue.toInt
					if (asInt != oldAddr) {
						dut.io.data.poke(rom(asInt) & 0xff)
						oldAddr = asInt
					}
					if ((i % 250) == 0)
						println(i)
					if (dut.io.error.peek().litValue != 0)
						break()
				}
			}
		}
	}
}
