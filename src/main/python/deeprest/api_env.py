import numpy as np
import gymnasium as gym
from gymnasium import spaces
import os

PRINT_LOG = False
OBS_MAX = 20 # It's an uint8! Choose 0-255

# Real API from RestTestGen
class ApiEnv(gym.Env):

    actions_count = 0
    observation = []

    def __init__(self):
        super(ApiEnv, self).__init__()

        # Create named pipes, if not already done.
        try:
            os.mkfifo('j2p')
        except:
            pass
        try:
            os.mkfifo('p2j')
        except:
            pass

        # Wait for number of available actions from Java
        print("Waiting for size of action space from Java.")
        with open('j2p', 'r') as read_fifo:
            line = read_fifo.read()
        self.actions_count = int(line)
        print(f"Received number of actions: {self.actions_count}")
        if (self.actions_count > 999):
            print("WARNING: more than 999 actions! Fix string encoding in step method.")

        # Init actions and observations spaces
        self.action_space = spaces.Discrete(self.actions_count)
        self.observation_space = spaces.Box(
            low=0,
            high=OBS_MAX,
            shape=(1, self.actions_count),
            dtype=np.uint8
        )

    def step(self, action):

        if PRINT_LOG:
            print(f"Next action is: {action}")

        # Send chosen action to Java
        with open('p2j', 'w') as write_fifo:
            write_fifo.write(f'{action:04}')

        if PRINT_LOG:
            print("Action sent! Waiting for outcome from Java.")

        # Read outcome from Java
        with open('j2p', 'r') as read_fifo:
            line = read_fifo.read()
        status_code = int(line)

        if PRINT_LOG:
            print(f"Operation tested with status code: {status_code}")

        # Termination: the agent reaches the goal
        # Truncation: the episode is truncated as the agent consumed all the budget without accomplishing the goal
        observation, reward, terminated, truncated = self.compute_observation_and_reward(action, status_code)

        if PRINT_LOG:
            print(observation)

        info = {}
        return observation, reward, terminated, truncated, info

    def reset(self, seed=None, options=None):
        # Nothing can be done to reset the API...
        # Just reset the observation
        self.observation = np.zeros(
            shape=(1, self.actions_count),
            dtype=np.uint8
        )
        info = {}
        return self.observation, info

    def render(self):
        return

    def close(self):
        return

    def compute_observation_and_reward(self, action, status_code):

        is_already_covered = self.observation[0][action] > 0
        is_successful = status_code >= 200 and status_code < 300
        truncated = False

        if is_successful:

            # Increase successes in observation (up to OBS_MAX)
            if self.observation[0][action] < OBS_MAX:
                self.observation[0][action] += 1

            # If an operation has been executed for more than OBS_MAX times, than trucate this episode
            else:
                truncated = True

            # Compute reward
            if is_already_covered:
                reward = -100
            else:
                reward = 1000

        else:

            # Compute reward
            if is_already_covered:
                reward = -100
            else:
                reward = -1

        terminated = True
        i = 0
        while i < self.actions_count:
            if self.observation[0][i] == 0:
                terminated = False
                break
            i += 1

        return self.observation, reward, terminated, truncated
