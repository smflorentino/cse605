package com.fiji.fivm.test;

import com.fiji.mvm.Payload;
import com.fiji.mvm.VMController;
import com.fiji.mvm.VMConfiguration;

import com.fiji.fivm.ThreadPriority;

import com.fiji.fivm.r1.StackAllocation;

import java.util.List;

public class RunScopedmemMVM {
    public static class VMSpawner implements Runnable {
	VMConfiguration vmc;

	public VMSpawner(Payload p, String... args) {
	    vmc = new VMConfiguration(p);
	    vmc.setArguments(args);
	}

	// This is @StackAllocation to more closely mimic timesliced VMs
	@StackAllocation
	public void run() {
	    VMController vm = new VMController();
	    vm.spawn(vmc);
	    Util.ensureEqual(vm.getLastResult(), 0);
	}
    }

    public static void main(String[] args) {
	System.out.println("Creating VM for scope pinned test");
	Payload simplescoped = Payload.getPayloadByName("simplegctestscoped");
	Thread scopethread = new Thread(new VMSpawner(simplescoped,
						      "10", "10000000", "10", "0"));

	System.out.println("Creating VM for static pinned test");
	Payload simplestatic = Payload.getPayloadByName("simplegcteststatic");
	Thread staticthread = new Thread(new VMSpawner(simplestatic,
						       "10", "10000000", "10"));

	System.out.println("Starting VM for scope pinned test");
	scopethread.start();
	System.out.println("Starting VM for static pinned test");
	staticthread.start();

	for (;;) {
	    try {
		scopethread.join();
		staticthread.join();
		break;
	    } catch (InterruptedException e) {
	    }
	}

	System.out.println("Both VMs finished successfully.");
    }
}
