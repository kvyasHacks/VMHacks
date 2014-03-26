/*================================================================================
Copyright (c) 2008 VMware, Inc. All Rights Reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, 
this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice, 
this list of conditions and the following disclaimer in the documentation 
and/or other materials provided with the distribution.

 * Neither the name of VMware, Inc. nor the names of its contributors may be used
to endorse or promote products derived from this software without specific prior 
written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL VMWARE, INC. OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.
================================================================================*/

package com.vmware.vim25.mo.samples;

import java.net.URL;

import org.tempuri.Service;
import org.tempuri.ServiceSoap;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import com.vmware.vim25.mo.samples.vm.VMSnapshot;

public class HelloVM 
{
	public static void main(String[] args) throws Exception
	{
		long start = System.currentTimeMillis();
		URL url = new URL("https://10.0.65.205/sdk");
		ServiceInstance si = new ServiceInstance(url, "root", "ram123anu", true);

		long end = System.currentTimeMillis();
		System.out.println("time taken:" + (end-start));
		Folder rootFolder = si.getRootFolder();
		String name = rootFolder.getName();
		System.out.println("root:" + name);
		ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
		if(mes==null || mes.length ==0)
		{
			return;
		}

		VirtualMachine vm = (VirtualMachine) mes[0]; 

		displayStatisticsData(vm);



		while(vm.getGuest().getIpAddress() != null) {

			String snapshotname = "backup_vm";

			Task task = vm.removeAllSnapshots_Task();

			if(task.waitForMe()==Task.SUCCESS) {

				System.out.println("Successfully removed snapshot " + snapshotname + " on " + vm.getName());
			}
			else {

				throw new Exception("Error creating snapshot!");
			}

			 task = vm.createSnapshot_Task(snapshotname, "auto", true, true);
			if(task.waitForMe()==Task.SUCCESS) {

				System.out.println("Successfully created snapshot " + snapshotname + " on " + vm.getName());
			}
			else {

				throw new Exception("Error creating snapshot!");
			}
			Thread.sleep(1000 * 60 * 1);
			displayStatisticsData(vm);

		}

		ManagedObjectReference hmor = vm.getRuntime().getHost();
		HostSystem host = new HostSystem(vm.getServerConnection(), hmor);
		
		displayStatisticsData(vm);

		//restore only if VM is running and not powered off
		if(vm.getGuest().getGuestState().equals("running")){
			
			restoreFromSnapshot(vm, rootFolder);
			
		} else if (! ping("https://10.0.65.205/").equals("$PING_RESULT")){
			// cold migration
		} else {
			//do nothing as vm is manually turned off 
		}
		
		 
		
		displayStatisticsData(vm);

	}

	public static void restoreFromSnapshot (VirtualMachine vm, Folder rootFolder )  throws Exception{
		

		VirtualMachine vmSrc = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", "anu1");
		VirtualMachineSnapshot vmsnap = VMSnapshot.getSnapshotInTree(vmSrc, "backup_vm");
		
		if(vmsnap!=null) {
			Task task = vmsnap.revertToSnapshot_Task(null);
			if(task.waitForMe()==Task.SUCCESS) {
				System.out.println("Reverted to snapshot:" );
			}
		}
	}

	public static void displayStatisticsData(VirtualMachine vm) throws Exception{

		vm.getResourcePool();
		System.out.println("\n --------------------------------------- \n" );
		System.out.println("Hello " + vm.getName());
		System.out.println("GuestOS: " + vm.getConfig().getGuestFullName());
		System.out.println("Multiple snapshot supported: " + vm.getCapability().isMultipleSnapshotsSupported());

		System.out.println("Vm Memory: " + vm.getSummary().quickStats.getHostMemoryUsage() + " MB");
		System.out.println("Overall CPU Usage: " + vm.getSummary().quickStats.getOverallCpuUsage() + " MHz");
		System.out.println("IP address: " + vm.getGuest().getIpAddress() );
		System.out.println("Network: " + vm.getNetworks()[0].getName());
		System.out.println(vm.getGuest().getGuestState());
		System.out.println(vm.getGuest().getNet());
		System.out.println("\n --------------------------------------- \n" );

	}

	public static String ping( String host )
	{
		System.out.println( "Ping Host: " + host ) ;
		Service service = new Service();
		ServiceSoap port = service.getServiceSoap(); 
		String result = port.pingHost( host ) ;
		System.out.println( "Ping Result: " + result ) ;
		return result;
	}

}
