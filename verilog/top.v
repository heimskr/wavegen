//Copyright 1986-2020 Xilinx, Inc. All Rights Reserved.
//--------------------------------------------------------------------------------
//Tool Version: Vivado v.2020.1 (win64) Build 2902540 Wed May 27 19:54:49 MDT 2020
//Date        : Thu Oct 22 12:57:52 2020
//Host        : WK142 running 64-bit major release  (build 9200)
//Command     : generate_target design_1_wrapper.bd
//Design      : design_1_wrapper
//Purpose     : IP block netlist
//--------------------------------------------------------------------------------
`timescale 1 ps / 1 ps

module top (
	input  clk,
	input  btnc,
	input  btnd,
	input  btnl,
	input  btnr,
	input  btnu,
	input  cpu_resetn,
	output [7:0] led,
	input  [7:0] sw,
	output ac_mclk,
	input  ac_adc_sdata,
	output ac_dac_sdata,
	output ac_bclk,
	output ac_lrclk,
	inout scl,
	inout sda,
	output oled_sclk,
	output oled_sdin,
	output oled_vbat,
	output oled_vdd,
	output oled_res,
	output oled_dc,
    output  hdmi_tx_cec,     // CE control bidirectional
    input   hdmi_tx_hpd,     // hot-plug detect
    output  hdmi_tx_rscl,    // DDC bidirectional
    output  hdmi_tx_rsda,    // DDC bidirectional
    output hdmi_tx_clk_n,   // HDMI clock differential negative
    output hdmi_tx_clk_p,   // HDMI clock differential positive
    output [2:0] hdmi_tx_n, // Three HDMI channels differential negative
    output [2:0] hdmi_tx_p  // Three HDMI channels differential positive
);

	// clk_wiz_1 clk_1 (
	// 	.clk_in1(clk),
	// 	.reset(cpu_resetn),
	// 	.clk_out1(clk_out_100MHZ),
	// 	.clk_out2(clk_out_200MHZ),
	// 	.clk_out3(ac_mclk),
	// 	.clk_out4(clk50),
	// 	.locked()
	// );

	// design_1_clk_wiz_0_0 clk_0 (
	// 	.clk_in1(clk),
	// 	.reset(!cpu_resetn),
	// 	.clk_out12(ac_mclk),
	// 	.clk_out50(clk50),
	// 	.locked()
	// );

	// wire clk50;

	wire clk12MHz;

	clk_wiz_0 clk_0 (
		.clk(clk),
		.reset(!cpu_resetn),
		.clk12(clk12MHz),
		.clk50()
	);

	wire clk_pix1;
	wire clk_pix5;

	clk_wiz_hdmi clk_hdmi (
		.clk(clk),
		.reset(!cpu_resetn),
		.clk_pix1(clk_pix1),
		.clk_pix5(clk_pix5)
	);

	assign ac_mclk = clk12MHz;

	// audio_init initialize_audio (
	// 	.clk(clk50),
	// 	.rst(!cpu_resetn),
	// 	.sda(sda),
	// 	.scl(scl)
	// );

	// wire [4:0] buttons_db;
	// wire [4:0] buttons_dbv;

	// debounce dbuttonsv (
	// 	.clock(clk),
	// 	.reset(!cpu_resetn),
	// 	.button(btn),
	// 	.out(buttons_dbv)
	// );

	// Debouncer dbuttons (
	// 	.clock(clk),
	// 	.reset(!cpu_resetn),
	// 	.io_in_0(btnu),
	// 	.io_in_1(btnr),
	// 	.io_in_2(btnl),
	// 	.io_in_3(btnd),
	// 	.io_in_4(btnc),
	// 	.io_out_0(buttons_db[0]),
	// 	.io_out_1(buttons_db[1]),
	// 	.io_out_2(buttons_db[2]),
	// 	.io_out_3(buttons_db[3]),
	// 	.io_out_4(buttons_db[4])
	// );

	wire [17:0] rom_addr;
	wire [23:0] rom_out;

//	RAM rom (
//		.clk(clk),
//		.addr(rom_addr),
//		.dout(rom_out)
//	);

	blk_mem_gen_0 rom (
		.clka(clk),
		.addra(rom_addr),
		.douta(rom_out)
	);

	wire [23:0] out_audioL;
	wire [23:0] out_audioR;
	// wire [23:0] in_audioL;
	// wire [23:0] in_audioR;

	// i2s_ctl audio_inout (
	// 	.CLK_I(clk),    // Sys clk
	// 	.RST_I(!cpu_resetn),    // Sys rst
	// 	.EN_TX_I(1),  // Transmit Enable (push sound data into chip)
	// 	.EN_RX_I(0), // Receive enable (pull sound data out of chip)
	// 	// .FS_I(4'b0101), // Sampling rate selector
	// 	.FS_I(4'b0000), // Sampling rate selector
	// 	// .FS_I(sw[3:0]), // Sampling rate selector
	// 	.MM_I(0),     // Audio controller Master mode select
	// 	.D_L_I({out_audioL, 8'b0}),    // Left channel data input from mix (mixed audio output)
	// 	.D_R_I({out_audioR, 8'b0}),   // Right channel data input from mix
	// 	.D_L_O(in_audioL),    // Left channel data (input from mic input)
	// 	.D_R_O(in_audioR),    // Right channel data (input from mic input)
	// 	.BCLK_O(ac_bclk),   // serial CLK
	// 	.LRCLK_O(ac_lrclk),  // channel CLK
	// 	.SDATA_O(ac_dac_sdata),  // Output serial data
	// 	.SDATA_I(ac_adc_sdata)   // Input serial data
	// );

	// wire gb_reset;
	// assign gb_reset = reset | btnd;

	MainGameBoy main_module (
	// Main main_module (
	// MainROMReader main_module (
		.clock(clk),
		.reset(!cpu_resetn),
		.io_buttonU(btnu),
		.io_buttonR(btnr),
		.io_buttonL(btnl),
		.io_buttonD(btnd),
		.io_buttonC(btnc),
		.io_sw(sw),
		.io_outL(out_audioL),
		.io_outR(out_audioR),
		.io_led(led),
		.io_addr(rom_addr),
		.io_rom(rom_out)
	);

	reg [23:0] storedL;
	reg [23:0] storedR;

	always @(posedge clk) begin
		storedL <= out_audioL;
		storedR <= out_audioR;
	end

	Display display (
		.clk(clk),
		.clk_pix1(clk_pix1),
		.clk_pix5(clk_pix5),
		.rst_n(cpu_resetn),
		.hdmi_tx_cec(hdmi_tx_cec),
		.hdmi_tx_hpd(hdmi_tx_hpd),
		.hdmi_tx_rscl(hdmi_tx_rscl),
		.hdmi_tx_rsda(hdmi_tx_rsda),
		.hdmi_tx_clk_n(hdmi_tx_clk_n),
		.hdmi_tx_clk_p(hdmi_tx_clk_p),
		.hdmi_tx_n(hdmi_tx_n),
		.hdmi_tx_p(hdmi_tx_p),
		.audioL(storedL),
		.audioR(storedR)
	);




	// reg is_on;

	// reg [7:0] dat;

	// reg [63:0] counter;
	// always @(posedge clk) begin
	// 	counter <= counter + 1;

	// 	out_audioL <= {1'b0, {23{counter[sw[7:2]]}}};
	// 	out_audioR <= {1'b0, {23{counter[sw[7:2]]}}};
	// 	is_on <= counter[sw[7:2]];

	// 	// if (counter[16]) begin
	// 	// 	out_audioL <= 24'b111111111111111111111111;
	// 	// 	out_audioR <= 24'b111111111111111111111111;
	// 	// 	is_on <= 1'b1;
	// 	// 	if (counter[15:0] == 16'b0)
	// 	// 		dat <= dat + 1;
	// 	// end else begin
	// 	// 	out_audioL <= 24'b000000000000000000000000;
	// 	// 	out_audioR <= 24'b000000000000000000000000;
	// 	// 	is_on <= 1'b0;
	// 	// end
	// end

	// // assign led[4:0] = ~buttons_db[4:0];
	// // assign led[7:0] = {8{is_on}};

	// reg [7:0] led_reg;

	// always @(*) begin
	// 	case (sw[1:0])
	// 		2'b00: led_reg[7:0] = dat[7:0];
	// 		2'b10: led_reg[7:0] = {8{is_on}};
	// 		2'b01: led_reg[7:0] = counter[7:0];
	// 		2'b11: led_reg[7:0] = counter[15:8];
	// 	endcase
	// end

	// assign led[7:0] = led_reg[7:0];

endmodule
