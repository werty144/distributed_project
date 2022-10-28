import os

  
def main():
	log_folder = os.path.join('..', 'logs')

	n_messages = -1
	with open(os.path.join(log_folder, 'config')) as f:
		n_messages = int(f.read().splitlines()[0].split()[0])

	n_processes = -1
	with open(os.path.join(log_folder, 'hosts')) as f:
		n_processes = len(f.read().splitlines())

	for file in os.listdir(log_folder):
		if file.endswith('.stderr'):
			with open(os.path.join(log_folder, file)) as f:
				content = f.read()
				if len(content) > 0:
					print(f'Error in {file}:\n {content}')

	messages = [set() for _ in range(n_processes - 1)]

	with open(os.path.join(log_folder, 'proc01.output')) as f:
		for line in f.read().splitlines():
			sender, msg = int(line.split()[1]) - 2, line.split()[2]
			if msg in messages[sender]:
				print('Duplicate!')
			elif not 1 <= int(msg) <= n_messages:
				print('Creation!')
			else:
				messages[sender].add(msg)

	for sender, msgs in enumerate(messages):
		print(f'{len(msgs)} from {sender + 2}')

	print()
	print(f'{sum(len(msgs) for msgs in messages)} in total')


if __name__ == '__main__':
	main()