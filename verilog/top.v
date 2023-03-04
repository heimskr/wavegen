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
	inout  scl,
	inout  sda,
	output oled_sclk,
	output oled_sdin,
	output oled_vbat,
	output oled_vdd,
	output oled_res,
	output oled_dc,
	output hdmi_tx_cec,     // CE control bidirectional
	input  hdmi_tx_hpd,     // hot-plug detect
	output hdmi_tx_rscl,    // DDC bidirectional
	output hdmi_tx_rsda,    // DDC bidirectional
	output hdmi_tx_clk_n,   // HDMI clock differential negative
	output hdmi_tx_clk_p,   // HDMI clock differential positive
	output [2:0] hdmi_tx_n, // Three HDMI channels differential negative
	output [2:0] hdmi_tx_p  // Three HDMI channels differential positive
);

	wire clk12MHz;
	wire clk30MHz;
	wire clk50MHz;

	clk_wiz_0 clk_0 (
		.clk(clk),
		.reset(!cpu_resetn),
		.clk12(clk12MHz),
		.clk30(clk30MHz),
		.clk50(clk50MHz)
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

	wire dbu;
	wire dbr;
	wire dbd;
	wire dbl;
	wire dbc;

	audio_init initialize_audio (
		.clk(clk50MHz),
		.rst(!cpu_resetn),
		.sda(sda),
		.scl(scl)
	);

	Debouncer5 dbuttons (
		.clock(clk),
		.reset(!cpu_resetn),
		.io_in_0(btnu),
		.io_in_1(btnr),
		.io_in_2(btnl),
		.io_in_3(btnd),
		.io_in_4(btnc),
		.io_out_0(dbu),
		.io_out_1(dbr),
		.io_out_2(dbl),
		.io_out_3(dbd),
		.io_out_4(dbc)
	);

	wire [17:0] rom_addr;
	wire [23:0] rom_out;

	blk_mem_gen_0 rom (
		.clka(clk),
		.addra(rom_addr),
		.douta(rom_out)
	);

	wire [23:0] out_audioL;
	wire [23:0] out_audioR;

	Main main_module (
		.clock(clk),
		.reset(!cpu_resetn),
		.io_pulseU(dbu),
		.io_pulseR(dbr),
		.io_pulseL(dbl),
		.io_pulseD(dbd),
		.io_pulseC(dbc),
		.io_sw(sw),
		.io_outL(out_audioL),
		.io_outR(out_audioR),
		.io_led(led),
		.io_addr(rom_addr),
		.io_rom(rom_out)
	);

	i2s_ctl audio_inout (
		.CLK_I(clk),    // Sys clk
		.RST_I(!cpu_resetn),    // Sys rst
		.EN_TX_I(1),  // Transmit Enable (push sound data into chip)
		.EN_RX_I(0), // Receive enable (pull sound data out of chip)
		// .FS_I(4'b0101), // Sampling rate selector
		.FS_I(4'b0000), // Sampling rate selector
		.MM_I(0),     // Audio controller Master mode select
		.D_L_I(out_audioL),    // Left channel data input from mix (mixed audio output)
		.D_R_I(out_audioR),   // Right channel data input from mix
		.D_L_O(in_audioL),    // Left channel data (input from mic input)
		.D_R_O(in_audioR),    // Right channel data (input from mic input)
		.BCLK_O(ac_bclk),   // serial CLK
		.LRCLK_O(ac_lrclk),  // channel CLK
		.SDATA_O(ac_dac_sdata),  // Output serial data
		.SDATA_I(ac_adc_sdata)   // Input serial data
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
		.sw(sw),
		.buttonL(btnl),
		.buttonR(btnr),
		.clk30(clk30MHz),
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

endmodule
